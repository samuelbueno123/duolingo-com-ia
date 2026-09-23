package com.duolinfo.ia.proj.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duolinfo.ia.proj.entity.ClassTeacherAssignment;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.ClassTeacherAssignmentRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassTeacherAssignmentService {

    private final ClassTeacherAssignmentRepository assignmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    public ClassTeacherAssignment assignTeacher(
        Long schoolClassId,
        Long teacherId,
        LocalDate startDate,
        String reason
    ) {

        SchoolClass schoolClass = findSchoolClass(schoolClassId);

        Teacher teacher = findTeacher(teacherId);

        /*
         * A turma e o professor precisam pertencer à mesma instituição.
         */
        Long classInstitutionId =
            schoolClass
                .getSchool()
                .getInstitution()
                .getId();

        Long teacherInstitutionId =
            teacher
                .getInstitution()
                .getId();

        if (!classInstitutionId.equals(teacherInstitutionId)) {
            throw new BusinessException(
                "O professor não pertence à mesma instituição da turma."
            );
        }

        LocalDate date =
            startDate != null
                ? startDate
                : LocalDate.now();

        /*
         * Procura o professor atual.
         */
        ClassTeacherAssignment current =
            assignmentRepository
                .findBySchoolClassIdAndEndDateIsNull(
                    schoolClassId
                )
                .orElse(null);

        if (current != null) {

            if (current.getTeacher().getId().equals(teacherId)) {
                throw new BusinessException(
                    "Esse professor já é o professor atual da turma."
                );
            }

            /*
             * Não permitimos colocar o novo professor em uma data
             * anterior ao início da atribuição atual.
             */
            if (!date.isAfter(current.getStartDate())) {
                throw new BusinessException(
                    "A data da nova atribuição deve ser posterior "
                    + "à data de início da atribuição atual."
                );
            }

            /*
             * Encerra o professor anterior no dia anterior.
             */
            current.setEndDate(date.minusDays(1));

            assignmentRepository.save(current);
        }

        ClassTeacherAssignment assignment =
            ClassTeacherAssignment.builder()
                .schoolClass(schoolClass)
                .teacher(teacher)
                .startDate(date)
                .reason(reason)
                .build();

        return assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public ClassTeacherAssignment findById(Long id) {

        return assignmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Atribuição de professor não encontrada: " + id
            ));
    }

    @Transactional(readOnly = true)
    public List<ClassTeacherAssignment> findByClass(
        Long schoolClassId
    ) {

        findSchoolClass(schoolClassId);

        return assignmentRepository
            .findBySchoolClassIdOrderByStartDateDesc(
                schoolClassId
            );
    }

    @Transactional(readOnly = true)
    public ClassTeacherAssignment findCurrentTeacher(
        Long schoolClassId
    ) {

        findSchoolClass(schoolClassId);

        return assignmentRepository
            .findBySchoolClassIdAndEndDateIsNull(
                schoolClassId
            )
            .orElseThrow(() -> new ResourceNotFoundException(
                "A turma não possui professor atualmente."
            ));
    }

    @Transactional(readOnly = true)
    public List<ClassTeacherAssignment> findByTeacher(
        Long teacherId
    ) {

        findTeacher(teacherId);

        return assignmentRepository
            .findByTeacherIdOrderByStartDateDesc(
                teacherId
            );
    }

    @Transactional
    public ClassTeacherAssignment finishCurrentAssignment(
        Long schoolClassId,
        LocalDate endDate
    ) {

        ClassTeacherAssignment current =
            assignmentRepository
                .findBySchoolClassIdAndEndDateIsNull(
                    schoolClassId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                    "A turma não possui professor atual."
                ));

        LocalDate date =
            endDate != null
                ? endDate
                : LocalDate.now();

        if (date.isBefore(current.getStartDate())) {
            throw new BusinessException(
                "A data de encerramento não pode ser anterior "
                + "à data de início."
            );
        }

        current.setEndDate(date);

        return assignmentRepository.save(current);
    }

    @Transactional
    public void delete(Long id) {

        ClassTeacherAssignment assignment = findById(id);

        /*
         * Não excluímos a atribuição atual.
         * Primeiro ela deve ser encerrada.
         */
        if (assignment.getEndDate() == null) {
            throw new BusinessException(
                "Não é possível excluir a atribuição atual. "
                + "Encerre a atribuição primeiro."
            );
        }

        assignmentRepository.delete(assignment);
    }

    private SchoolClass findSchoolClass(Long id) {

        return schoolClassRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Turma não encontrada: " + id
            ));
    }

    private Teacher findTeacher(Long id) {

        return teacherRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Professor não encontrado: " + id
            ));
    }
}