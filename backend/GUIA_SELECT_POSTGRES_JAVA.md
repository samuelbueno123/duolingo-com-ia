# Consultas `SELECT` no PostgreSQL com Java

Este guia mostra como consultar o PostgreSQL do projeto e passar valores com segurança usando `?`, do mesmo jeito que parâmetros preparados no PHP com PDO.

## 1. Inicie o PostgreSQL

Na pasta `backend`, inicie o serviço do banco:

```powershell
docker compose up -d postgres
```

Se a aplicação Java estiver rodando fora do Docker, ela se conecta em `localhost:5432`. Se a aplicação estiver no Compose, o endereço do banco é `postgres:5432`.

## 2. A forma já usada pelo projeto: Spring Data JPA

O projeto já tem `UserRepository` com o método `findByEmail`. Para buscar um usuário, injete o repositório no seu serviço e chame o método:

```java
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> buscarPorEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
```

O Spring Data cria a consulta e trata o valor como parâmetro. Para buscas simples como essa, prefira o repositório existente. Use SQL manual quando precisar controlar a consulta diretamente.

## 3. SQL manual com `JdbcTemplate`

`JdbcTemplate` permite escrever o `SELECT` e usar `?` como marcador para cada valor. Exemplo de método de repositório:

```java
@Repository
public class UserSqlRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserSqlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserSummary> buscarPorEmail(String email) {
        String sql = """
            SELECT id, name, email
            FROM users
            WHERE email = ?
            """;

        List<UserSummary> encontrados = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new UserSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
            ),
            email
        );

        return encontrados.stream().findFirst();
    }
}

public record UserSummary(Long id, String name, String email) {}
```

Os argumentos depois do `RowMapper` são associados aos `?` pela ordem: o primeiro argumento preenche o primeiro `?`, o segundo preenche o segundo, e assim por diante. O `JdbcTemplate` cuida de abrir e fechar os recursos JDBC usados na consulta.

Uma busca por parte do nome pode ser escrita assim:

```java
String sql = "SELECT id, name, email FROM users WHERE name ILIKE ?";
String filtro = "%" + nome + "%";
List<UserSummary> encontrados = jdbcTemplate.query(sql, rowMapper, filtro);
```

`ILIKE` faz a busca sem diferenciar maiúsculas e minúsculas no PostgreSQL. O `%` é parte do valor enviado como parâmetro; não concatene o conteúdo recebido do usuário no texto SQL.

## 4. A mesma consulta com JDBC puro

Se estiver usando `DataSource` diretamente, prepare a consulta e associe o parâmetro antes de executá-la:

```java
String sql = "SELECT id, name, email FROM users WHERE email = ?";

try (Connection connection = dataSource.getConnection();
     PreparedStatement statement = connection.prepareStatement(sql)) {

    statement.setString(1, email); // Os índices de parâmetros começam em 1.

    try (ResultSet resultado = statement.executeQuery()) {
        if (resultado.next()) {
            long id = resultado.getLong("id");
            String nome = resultado.getString("name");
            String emailEncontrado = resultado.getString("email");
        }
    }
}
```

Para um `SELECT`, use `executeQuery()`, que retorna um `ResultSet`. Para comandos que alteram dados, como `INSERT`, `UPDATE` e `DELETE`, JDBC usa normalmente `executeUpdate()`.

## 5. Comparação com PHP PDO

Em PHP, o padrão é preparar a consulta e então passar os valores:

```php
$stmt = $pdo->prepare(
    'SELECT id, name, email FROM users WHERE email = ?'
);
$stmt->execute([$email]);
$usuario = $stmt->fetch();
```

Em Java com `PreparedStatement`, a ideia é equivalente:

```java
PreparedStatement statement = connection.prepareStatement(
    "SELECT id, name, email FROM users WHERE email = ?"
);
statement.setString(1, email);
ResultSet resultado = statement.executeQuery();
```

Também é possível usar parâmetros nomeados em PDO, mas o `?` posicional acima corresponde diretamente ao marcador usado por JDBC e `JdbcTemplate`.

## 6. Por que isso ajuda contra SQL injection?

Não monte SQL concatenando dados recebidos do usuário:

```java
// Evite: o texto recebido vira parte da instrução SQL.
String sql = "SELECT id, name FROM users WHERE email = '" + email + "'";
```

Separe a instrução dos valores:

```java
String sql = "SELECT id, name FROM users WHERE email = ?";
jdbcTemplate.query(sql, rowMapper, email);
```

O banco recebe o comando e os valores como parâmetros. Assim, um valor contendo aspas ou trechos que parecem SQL continua sendo tratado como dado, em vez de alterar a estrutura da consulta.

O `?` serve para valores, como email, nome ou id. Ele não substitui nomes de tabela, nomes de coluna nem palavras SQL. Para ordenar por uma escolha do usuário, converta essa escolha por uma lista permitida:

```java
String colunaOrdenacao = switch (ordenarPor) {
    case "nome" -> "name";
    case "email" -> "email";
    default -> "id";
};

String sql = "SELECT id, name, email FROM users ORDER BY " + colunaOrdenacao;
```

Aqui a concatenação é segura porque `colunaOrdenacao` só pode receber uma das opções definidas no código. Nunca use diretamente um nome de coluna recebido do usuário.

## 7. Um detalhe: `?` no Java e `$1` no PostgreSQL

No Java usando JDBC ou `JdbcTemplate`, escreva `?`; o driver PostgreSQL faz o vínculo dos parâmetros. Se você escrever uma instrução `PREPARE` diretamente no `psql` ou em uma ferramenta SQL, a sintaxe nativa do PostgreSQL usa `$1`, `$2`, etc.:

```sql
PREPARE buscar_usuario(text) AS
    SELECT id, name, email FROM users WHERE email = $1;

EXECUTE buscar_usuario('pessoa@example.com');
```

No código Java normal, você não precisa escrever `PREPARE` ou `$1`: use `?` e os métodos de parâmetros mostrados acima.

## Referências oficiais

- [Java 25: `PreparedStatement`](https://docs.oracle.com/en/java/javase/25/docs/api/java.sql/java/sql/PreparedStatement.html)
- [Spring Framework: acesso a dados com `JdbcTemplate`](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html)
- [PostgreSQL: `PREPARE`](https://www.postgresql.org/docs/current/sql-prepare.html)
