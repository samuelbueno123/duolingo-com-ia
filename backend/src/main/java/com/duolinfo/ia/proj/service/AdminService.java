package com.duolinfo.ia.proj.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.AdminUserRequestDTO;
import com.duolinfo.ia.proj.dto.request.InstitutionAdminRoleRequestDTO;
import com.duolinfo.ia.proj.dto.response.AdminTableColumnResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminTableDataResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminTableMetadataResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminUserResponseDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionAdminDashboardDTO;
import com.duolinfo.ia.proj.dto.response.SchoolClassResponseDTO;
import com.duolinfo.ia.proj.dto.response.SchoolResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentResponseDTO;
import com.duolinfo.ia.proj.dto.response.TeacherResponseDTO;
import com.duolinfo.ia.proj.entity.Institution;
import com.duolinfo.ia.proj.entity.School;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.mapper.DtoMapper;
import com.duolinfo.ia.proj.repository.InstitutionRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.SchoolRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

@Service
public class AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);
    private static final String SCHEMA = "public";
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> SYSTEM_TABLES = Set.of(
        "flyway_schema_history",
        "databasechangelog",
        "databasechangeloglock"
    );
    private static final Set<String> SENSITIVE_COLUMNS = Set.of(
        "password_hash",
        "access_token",
        "refresh_token",
        "secret",
        "client_secret"
    );
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 250;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final AuthService authService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherService teacherService;
    private final InstitutionRepository institutionRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;

    public AdminService(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            UserService userService,
            AuthService authService,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            TeacherService teacherService,
            InstitutionRepository institutionRepository,
            SchoolRepository schoolRepository,
            SchoolClassRepository schoolClassRepository) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.authService = authService;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.teacherService = teacherService;
        this.institutionRepository = institutionRepository;
        this.schoolRepository = schoolRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    public void requireSuperAdmin(String adminEmail) {
        User caller = adminEmail == null ? null : userService.findByEmail(adminEmail);
        if (caller == null || !caller.isSuperAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Apenas o administrador geral (admin@duolingo.com) possui permissão para esta operação.");
        }
    }

    public Long resolveScopedInstitutionId(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) return null;
        User caller = userService.findByEmail(adminEmail);
        if (caller == null || caller.isSuperAdmin()) {
            return null;
        }
        Teacher teacher = teacherRepository.findById(caller.getId()).orElse(null);
        if (teacher != null && teacher.getInstitution() != null) {
            return teacher.getInstitution().getId();
        }
        return null;
    }

    public List<AdminTableMetadataResponseDTO> listTables() {
        List<String> tableNames = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(
                    connection.getCatalog(), SCHEMA, "%", new String[] { "TABLE" })) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (!SYSTEM_TABLES.contains(tableName.toLowerCase(Locale.ROOT))) {
                        tableNames.add(tableName);
                    }
                }
            }
        } catch (SQLException exception) {
            throw databaseUnavailable(exception);
        }

        return tableNames.stream()
            .map(this::describeTable)
            .map(table -> new AdminTableMetadataResponseDTO(
                table.name(),
                labelFor(table.name()),
                countRows(table),
                table.primaryKeys(),
                table.columns().stream().map(ColumnDefinition::response).toList(),
                isReadOnly(table.name())))
            .sorted(Comparator.comparing(AdminTableMetadataResponseDTO::tableName))
            .toList();
    }

    public AdminTableDataResponseDTO getTableRows(
            String tableName,
            int requestedLimit,
            int requestedOffset,
            String search) {
        TableDefinition table = describeTable(tableName);
        int limit = requestedLimit <= 0
            ? DEFAULT_PAGE_SIZE
            : Math.min(requestedLimit, MAX_PAGE_SIZE);
        int offset = Math.max(requestedOffset, 0);

        List<ColumnDefinition> searchableColumns = table.columns().stream()
            .filter(column -> isSearchable(column.jdbcType(), column.response().udtName()))
            .toList();
        String searchText = search == null ? "" : search.trim();
        String whereClause = "";
        List<Object> parameters = new ArrayList<>();
        if (!searchText.isEmpty() && !searchableColumns.isEmpty()) {
            whereClause = " WHERE (" + searchableColumns.stream()
                .map(column -> "CAST(" + quote(column.response().name()) + " AS TEXT) ILIKE ?")
                .collect(java.util.stream.Collectors.joining(" OR ")) + ")";
            String pattern = "%" + searchText + "%";
            searchableColumns.forEach(column -> parameters.add(pattern));
        }

        String tableSql = qualifiedTable(table.name());
        Long totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + tableSql + whereClause,
            Long.class,
            parameters.toArray());

        String query = "SELECT * FROM " + tableSql
            + whereClause + " LIMIT ? OFFSET ?";
        parameters.add(limit);
        parameters.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.query(
            query,
            (resultSet, rowNumber) -> toRow(resultSet, table.columns()),
            parameters.toArray());

        return new AdminTableDataResponseDTO(
            table.name(),
            labelFor(table.name()),
            totalCount == null ? 0 : totalCount,
            table.primaryKeys(),
            table.columns().stream().map(ColumnDefinition::response).toList(),
            rows,
            isReadOnly(table.name()));
    }

    @Transactional
    public Map<String, Object> insertRow(String tableName, Map<String, Object> values) {
        TableDefinition table = writableTable(tableName);
        Map<ColumnDefinition, Object> convertedValues = convertValues(table, values);
        String sql;

        if (convertedValues.isEmpty()) {
            sql = "INSERT INTO " + qualifiedTable(table.name()) + " DEFAULT VALUES";
            jdbcTemplate.update(sql);
        } else {
            List<ColumnDefinition> columns = List.copyOf(convertedValues.keySet());
            String columnNames = columns.stream()
                .map(column -> quote(column.response().name()))
                .collect(java.util.stream.Collectors.joining(", "));
            String placeholders = columns.stream()
                .map(column -> "CAST(? AS " + castType(column) + ")")
                .collect(java.util.stream.Collectors.joining(", "));
            sql = "INSERT INTO " + qualifiedTable(table.name()) + " (" + columnNames
                + ") VALUES (" + placeholders + ")";
            jdbcTemplate.update(sql, convertedValues.values().toArray());
        }
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> updateRow(
            String tableName,
            Map<String, Object> keys,
            Map<String, Object> values) {
        TableDefinition table = writableTable(tableName);
        List<ColumnDefinition> primaryKeyColumns = requirePrimaryKeyValues(table, keys);
        Map<ColumnDefinition, Object> convertedValues = convertValues(table, values);
        convertedValues.keySet().removeIf(column -> column.response().isPrimaryKey());
        if (convertedValues.isEmpty()) {
            throw badRequest("Informe ao menos um campo para atualizar.");
        }

        String assignments = convertedValues.keySet().stream()
            .map(column -> quote(column.response().name()) + " = CAST(? AS "
                + castType(column) + ")")
            .collect(java.util.stream.Collectors.joining(", "));
        String where = primaryKeyColumns.stream()
            .map(column -> quote(column.response().name()) + " = CAST(? AS "
                + castType(column) + ")")
            .collect(java.util.stream.Collectors.joining(" AND "));
        List<Object> parameters = new ArrayList<>(convertedValues.values());
        parameters.addAll(keyValues(primaryKeyColumns, keys));

        int updated = jdbcTemplate.update(
            "UPDATE " + qualifiedTable(table.name()) + " SET " + assignments
                + " WHERE " + where,
            parameters.toArray());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado.");
        }
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> deleteRow(String tableName, Map<String, Object> keys) {
        TableDefinition table = writableTable(tableName);
        List<ColumnDefinition> primaryKeyColumns = requirePrimaryKeyValues(table, keys);
        String where = primaryKeyColumns.stream()
            .map(column -> quote(column.response().name()) + " = CAST(? AS "
                + castType(column) + ")")
            .collect(java.util.stream.Collectors.joining(" AND "));
        int deleted = jdbcTemplate.update(
            "DELETE FROM " + qualifiedTable(table.name()) + " WHERE " + where,
            keyValues(primaryKeyColumns, keys).toArray());
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado.");
        }
        return Map.of("success", true);
    }

    @Transactional
    public List<AdminUserResponseDTO> listUsers(String currentAdminEmail) {
        Long scopedInstId = resolveScopedInstitutionId(currentAdminEmail);
        return userService.findAll().stream()
            .map(this::toAdminUser)
            .filter(userDto -> scopedInstId == null || Objects.equals(scopedInstId, userDto.institutionId()))
            .toList();
    }

    @Transactional
    public AdminUserResponseDTO createUser(AdminUserRequestDTO request, String currentAdminEmail) {
        if (request.password() == null || request.password().isBlank()) {
            throw badRequest("A senha é obrigatória para criar um usuário.");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(request.password());
        user.setGender(User.normalizeGender(request.gender()));
        user.setAdmin(request.isAdmin());
        User createdUser = userService.create(user);
        if (request.isAdmin()) {
            teacherService.ensureAdminTeacherProfile(createdUser.getId());
        }
        return toAdminUser(createdUser);
    }

    @Transactional
    public AdminUserResponseDTO updateUser(
            Long userId,
            AdminUserRequestDTO request,
            String currentAdminEmail) {
        User user = userService.findById(userId);
        ensureAdminCanChangeRole(user, request.isAdmin(), currentAdminEmail);
        boolean wasAdmin = user.isAdmin() || authService.isConfiguredAdmin(user);

        user.setName(request.name());
        user.setEmail(request.email());
        if (request.gender() != null && !request.gender().isBlank()) {
            user.setGender(User.normalizeGender(request.gender()));
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(request.password());
        }
        user.setAdmin(request.isAdmin());
        User updatedUser = userService.update(user);
        if (request.isAdmin() || wasAdmin) {
            teacherService.ensureAdminTeacherProfile(updatedUser.getId());
        }
        return toAdminUser(updatedUser);
    }

    @Transactional
    public AdminUserResponseDTO setAdminRole(
            Long userId,
            boolean isAdmin,
            String currentAdminEmail) {
        User user = userService.findById(userId);
        ensureAdminCanChangeRole(user, isAdmin, currentAdminEmail);
        boolean wasAdmin = user.isAdmin() || authService.isConfiguredAdmin(user);
        user.setAdmin(isAdmin);
        User updatedUser = userService.update(user);
        Teacher teacher = teacherRepository.findById(userId).orElse(null);
        if (teacher != null) {
            if (!isAdmin && !teacher.isPrimaryInstitutionAdmin()) {
                teacher.setInstitutionAdmin(false);
            } else if (isAdmin) {
                teacher.setInstitutionAdmin(true);
                teacher.setApproved(true);
                teacher.setApprovalStatus("APPROVED");
            }
            teacherRepository.save(teacher);
        } else if (isAdmin || wasAdmin) {
            teacherService.ensureAdminTeacherProfile(updatedUser.getId());
        }
        return toAdminUser(updatedUser);
    }

    @Transactional
    public TeacherResponseDTO setInstitutionAdminRole(
            Long teacherId,
            InstitutionAdminRoleRequestDTO request,
            String currentAdminEmail) {
        requireSuperAdmin(currentAdminEmail);
        Teacher teacher = teacherService.findById(teacherId);
        if (teacher.isSuperAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A conta geral de administrador (admin@duolingo.com) não pode ser alterada por aqui.");
        }
        boolean instAdmin = request.institutionAdmin();
        boolean primary = instAdmin && (request.primaryInstitutionAdmin() != null
            ? request.primaryInstitutionAdmin()
            : teacher.isPrimaryInstitutionAdmin());

        teacher.setInstitutionAdmin(instAdmin);
        teacher.setPrimaryInstitutionAdmin(primary);
        teacher.setAdmin(instAdmin);
        if (instAdmin) {
            teacher.setApproved(true);
            teacher.setApprovalStatus("APPROVED");
        }
        Teacher saved = teacherRepository.save(teacher);
        return DtoMapper.toTeacherResponse(saved);
    }

    @Transactional(readOnly = true)
    public InstitutionAdminDashboardDTO getInstitutionDashboard(
            Long requestedInstitutionId,
            String currentAdminEmail) {
        Long scopedInstId = resolveScopedInstitutionId(currentAdminEmail);
        Long targetInstId = scopedInstId != null ? scopedInstId : requestedInstitutionId;

        if (scopedInstId != null && requestedInstitutionId != null && !Objects.equals(scopedInstId, requestedInstitutionId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Administradores de instituição só podem visualizar os dados de sua própria instituição.");
        }

        Institution institution;
        if (targetInstId != null) {
            institution = institutionRepository.findById(targetInstId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada."));
        } else {
            institution = institutionRepository.findByCode(InstitutionService.DEFAULT_INSTITUTION_CODE)
                .orElseGet(() -> institutionRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma instituição cadastrada.")));
        }

        Long instId = institution.getId();
        List<School> schools = schoolRepository.findByInstitutionId(instId);

        List<SchoolClassResponseDTO> classDtos = new ArrayList<>();
        for (School school : schools) {
            schoolClassRepository.findBySchoolId(school.getId()).stream()
                .map(DtoMapper::toSchoolClassResponse)
                .forEach(classDtos::add);
        }

        List<Teacher> teachers = teacherRepository.findByInstitutionId(instId).stream()
            .filter(t -> !t.isSuperAdmin())
            .toList();
        List<TeacherResponseDTO> teacherDtos = teachers.stream().map(DtoMapper::toTeacherResponse).toList();
        int pendingTeachers = (int) teachers.stream().filter(t -> !Boolean.TRUE.equals(t.getApproved())).count();

        List<Student> students = studentRepository.findByInstitutionId(instId);
        List<StudentResponseDTO> studentDtos = students.stream().map(DtoMapper::toStudentResponse).toList();

        return new InstitutionAdminDashboardDTO(
            DtoMapper.toInstitutionResponse(institution),
            teacherDtos.size(),
            pendingTeachers,
            studentDtos.size(),
            classDtos.size(),
            0,
            teacherDtos,
            studentDtos,
            classDtos,
            List.of(),
            List.of()
        );
    }

    @Transactional
    public void deleteUser(Long userId, String currentAdminEmail) {
        User user = userService.findById(userId);
        if (user.isSuperAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "A conta geral de administrador (admin@duolingo.com) é superior a todas as outras contas e não pode ser excluída.");
        }
        ensureAdminCanChangeRole(user, false, currentAdminEmail);
        userService.deleteById(userId);
    }

    private AdminUserResponseDTO toAdminUser(User user) {
        List<String> roles = authService.resolveRoles(user);
        Teacher teacher = teacherRepository.findById(user.getId()).orElse(null);
        Student student = teacher == null ? studentRepository.findById(user.getId()).orElse(null) : null;
        String baseProfileType = teacher != null
            ? "TEACHER"
            : student != null ? "STUDENT" : "USER";
        Long instId = teacher != null && teacher.getInstitution() != null
            ? teacher.getInstitution().getId()
            : (student != null && student.getInstitution() != null ? student.getInstitution().getId() : null);
        String instName = teacher != null && teacher.getInstitution() != null
            ? teacher.getInstitution().getName()
            : (student != null && student.getInstitution() != null ? student.getInstitution().getName() : null);

        return new AdminUserResponseDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            roles.contains("ADMIN"),
            baseProfileType,
            roles.get(0),
            user.getGoogleId(),
            user.getProfilePicture(),
            user.getNormalizedGender(),
            user.isSuperAdmin(),
            teacher != null && teacher.isInstitutionAdmin(),
            teacher != null && teacher.isPrimaryInstitutionAdmin(),
            instId,
            instName);
    }

    private void ensureAdminCanChangeRole(
            User target,
            boolean requestedAdmin,
            String currentAdminEmail) {
        if (target.isSuperAdmin()) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "A conta geral de administrador (admin@duolingo.com) é superior a todas as outras contas e não pode ser rebaixada em hipótese alguma.");
        }
        if ( requestedAdmin && studentRepository.existsById(target.getId()) ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Contas de aluno não podem receber cargo de administrador.");
        }

        User caller = currentAdminEmail == null ? null : userService.findByEmail(currentAdminEmail);
        boolean callerIsSuperAdmin = caller != null && caller.isSuperAdmin();

        boolean sameUser = target.getEmail() != null
            && target.getEmail().equalsIgnoreCase(currentAdminEmail == null ? "" : currentAdminEmail);
        if (!requestedAdmin && sameUser) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Um administrador não pode remover o próprio acesso.");
        }

        Teacher targetTeacher = teacherRepository.findById(target.getId()).orElse(null);
        if (!callerIsSuperAdmin) {
            if (targetTeacher != null && targetTeacher.isPrimaryInstitutionAdmin() && !requestedAdmin) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A primeira conta registrada na instituição possui status elevado de administrador e não pode ser rebaixada por outros administradores desta instituição.");
            }
            if (requestedAdmin != (target.isAdmin() || (targetTeacher != null && targetTeacher.isInstitutionAdmin()))) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Apenas o administrador geral (admin@duolingo.com) tem o poder de alterar administradores de instituições.");
            }
        }

        if (!requestedAdmin && authService.isConfiguredAdmin(target)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Este acesso é definido pela lista de administradores configurada no servidor.");
        }
    }

    private TableDefinition writableTable(String tableName) {
        TableDefinition table = describeTable(tableName);
        if (isReadOnly(table.name())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Gerencie usuários pela seção Usuários & ADMs.");
        }
        return table;
    }

    private TableDefinition describeTable(String tableName) {
        if (tableName == null || !SAFE_IDENTIFIER.matcher(tableName).matches()) {
            throw badRequest("Nome de tabela inválido.");
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String actualName = null;
            try (ResultSet tables = metadata.getTables(
                    connection.getCatalog(), SCHEMA, tableName, new String[] { "TABLE" })) {
                while (tables.next()) {
                    String candidate = tables.getString("TABLE_NAME");
                    if (candidate.equals(tableName)) {
                        actualName = candidate;
                        break;
                    }
                }
            }
            if (actualName == null || SYSTEM_TABLES.contains(actualName.toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela não encontrada.");
            }

            Map<String, Short> primaryKeyOrder = new LinkedHashMap<>();
            try (ResultSet keys = metadata.getPrimaryKeys(connection.getCatalog(), SCHEMA, actualName)) {
                while (keys.next()) {
                    primaryKeyOrder.put(keys.getString("COLUMN_NAME"), keys.getShort("KEY_SEQ"));
                }
            }

            List<ColumnDefinition> columns = new ArrayList<>();
            try (ResultSet result = metadata.getColumns(connection.getCatalog(), SCHEMA, actualName, "%")) {
                while (result.next()) {
                    String name = result.getString("COLUMN_NAME");
                    String typeName = result.getString("TYPE_NAME");
                    String defaultValue = result.getString("COLUMN_DEF");
                    boolean autoIncrement = "YES".equalsIgnoreCase(result.getString("IS_AUTOINCREMENT"))
                        || (defaultValue != null && defaultValue.toLowerCase(Locale.ROOT).contains("nextval("));
                    AdminTableColumnResponseDTO response = new AdminTableColumnResponseDTO(
                        name,
                        typeName,
                        typeName,
                        result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                        primaryKeyOrder.containsKey(name),
                        autoIncrement,
                        defaultValue);
                    columns.add(new ColumnDefinition(response, result.getInt("DATA_TYPE")));
                }
            }
            List<String> primaryKeys = primaryKeyOrder.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
            return new TableDefinition(actualName, columns, primaryKeys);
        } catch (SQLException exception) {
            throw databaseUnavailable(exception);
        }
    }

    private long countRows(TableDefinition table) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + qualifiedTable(table.name()), Long.class);
        return count == null ? 0 : count;
    }

    private Map<String, Object> toRow(
            ResultSet resultSet,
            List<ColumnDefinition> columns) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (ColumnDefinition column : columns) {
            String name = column.response().name();
            Object value;
            if (isBinary(column)) {
                byte[] bytes = resultSet.getBytes(name);
                value = bytes == null ? null : "[BYTEA: " + bytes.length + " bytes]";
            } else {
                value = resultSet.getObject(name);
            }
            if (value != null
                    && "org.postgresql.util.PGobject".equals(value.getClass().getName())) {
                value = value.toString();
            }
            row.put(name, value);
        }
        return row;
    }

    private Map<ColumnDefinition, Object> convertValues(
            TableDefinition table,
            Map<String, Object> values) {
        if (values == null) {
            throw badRequest("Os valores do registro não foram informados.");
        }
        Map<String, ColumnDefinition> columnsByName = new LinkedHashMap<>();
        table.columns().forEach(column -> columnsByName.put(column.response().name(), column));

        Map<ColumnDefinition, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            ColumnDefinition column = columnsByName.get(entry.getKey());
            if (column == null) {
                throw badRequest("Campo desconhecido: " + entry.getKey());
            }
            if (isBinary(column) || isSensitive(entry.getKey())) {
                throw badRequest("Este campo não pode ser alterado pelo editor de tabelas.");
            }
            if (column.response().isAutoIncrement() && isEmpty(entry.getValue())) {
                continue;
            }
            converted.put(column, convertValue(column, entry.getValue()));
        }
        return converted;
    }

    private Object convertValue(ColumnDefinition column, Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty() && column.response().nullable()) {
            return null;
        }
        try {
            return switch (column.jdbcType()) {
                case Types.BOOLEAN, Types.BIT -> parseBoolean(text);
                case Types.TINYINT, Types.SMALLINT -> Short.valueOf(text);
                case Types.INTEGER -> Integer.valueOf(text);
                case Types.BIGINT -> Long.valueOf(text);
                case Types.NUMERIC, Types.DECIMAL -> new BigDecimal(text);
                case Types.REAL, Types.FLOAT -> Float.valueOf(text);
                case Types.DOUBLE -> Double.valueOf(text);
                case Types.DATE -> Date.valueOf(text);
                case Types.TIMESTAMP -> Timestamp.valueOf(LocalDateTime.parse(text.replace(' ', 'T')));
                case Types.TIMESTAMP_WITH_TIMEZONE -> OffsetDateTime.parse(text);
                case Types.OTHER -> convertOtherType(column, value, text);
                case Types.ARRAY -> throw badRequest("Campos de lista não podem ser editados por aqui.");
                default -> value instanceof String ? value : text;
            };
        } catch (IllegalArgumentException exception) {
            throw badRequest("Valor inválido para o campo " + column.response().name() + ".");
        }
    }

    private Object convertOtherType(ColumnDefinition column, Object value, String text) {
        String type = column.response().udtName().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "uuid" -> UUID.fromString(text);
            case "json", "jsonb" -> value instanceof String ? value : value.toString();
            default -> throw badRequest("Tipo de campo não suportado para edição: " + type);
        };
    }

    private String castType(ColumnDefinition column) {
        String type = column.response().udtName().toLowerCase(Locale.ROOT);
        return switch (column.jdbcType()) {
            case Types.BOOLEAN, Types.BIT -> "BOOLEAN";
            case Types.TINYINT, Types.SMALLINT -> "SMALLINT";
            case Types.INTEGER -> "INTEGER";
            case Types.BIGINT -> "BIGINT";
            case Types.NUMERIC, Types.DECIMAL -> "NUMERIC";
            case Types.REAL, Types.FLOAT -> "REAL";
            case Types.DOUBLE -> "DOUBLE PRECISION";
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR,
                Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> "TEXT";
            case Types.DATE -> "DATE";
            case Types.TIME -> "TIME";
            case Types.TIME_WITH_TIMEZONE -> "TIMETZ";
            case Types.TIMESTAMP -> "TIMESTAMP";
            case Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMPTZ";
            case Types.OTHER -> switch (type) {
                case "uuid" -> "UUID";
                case "json" -> "JSON";
                case "jsonb" -> "JSONB";
                default -> throw badRequest("Tipo de campo não suportado para edição: " + type);
            };
            default -> throw badRequest("Tipo de campo não suportado para edição.");
        };
    }

    private List<ColumnDefinition> requirePrimaryKeyValues(
            TableDefinition table,
            Map<String, Object> keys) {
        if (table.primaryKeys().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Esta tabela não possui chave primária e não permite alterar registros.");
        }
        if (keys == null || !keys.keySet().equals(Set.copyOf(table.primaryKeys()))) {
            throw badRequest("Informe todos os campos da chave primária do registro.");
        }
        Map<String, ColumnDefinition> columns = new LinkedHashMap<>();
        table.columns().forEach(column -> columns.put(column.response().name(), column));
        return table.primaryKeys().stream().map(columns::get).toList();
    }

    private List<Object> keyValues(List<ColumnDefinition> keyColumns, Map<String, Object> keys) {
        return keyColumns.stream()
            .map(column -> convertValue(column, keys.get(column.response().name())))
            .toList();
    }

    private static boolean isReadOnly(String tableName) {
        return "users".equalsIgnoreCase(tableName);
    }

    private static boolean isSensitive(String columnName) {
        String lowerName = columnName.toLowerCase(Locale.ROOT);
        return SENSITIVE_COLUMNS.contains(lowerName)
            || lowerName.endsWith("_secret")
            || lowerName.endsWith("_token");
    }

    private static boolean isBinary(ColumnDefinition column) {
        String udt = column.response().udtName().toLowerCase(Locale.ROOT);
        return udt.equals("bytea")
            || udt.equals("oid")
            || switch (column.jdbcType()) {
                case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY,
                    Types.BLOB, Types.CLOB, Types.NCLOB -> true;
                default -> false;
            };
    }

    private static boolean isSearchable(int jdbcType, String udtName) {
        return switch (jdbcType) {
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR,
                Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> true;
            default -> "text".equalsIgnoreCase(udtName)
                || "citext".equalsIgnoreCase(udtName);
        };
    }

    private static boolean isEmpty(Object value) {
        return value == null || value.toString().isBlank();
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw badRequest("Use true ou false para campos booleanos.");
    }

    private static String qualifiedTable(String name) {
        return quote(SCHEMA) + "." + quote(name);
    }

    private static String quote(String identifier) {
        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw badRequest("Identificador de banco inválido.");
        }
        return "\"" + identifier + "\"";
    }

    private static String labelFor(String tableName) {
        String words = tableName.replace('_', ' ');
        return words.isEmpty()
            ? words
            : words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
    }

    private RuntimeException databaseUnavailable(SQLException exception) {
        LOGGER.error("Não foi possível consultar o banco pelo painel administrativo.", exception);
        return new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Não foi possível acessar o banco de dados. Verifique se o PostgreSQL está ativo.",
            exception);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record ColumnDefinition(
        AdminTableColumnResponseDTO response,
        int jdbcType) {
    }

    private record TableDefinition(
        String name,
        List<ColumnDefinition> columns,
        List<String> primaryKeys) {
    }
}
