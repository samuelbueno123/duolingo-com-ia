package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.TeacherStudentNoteRequestDTO;
import com.duolinfo.ia.proj.dto.response.TeacherStudentNoteResponseDTO;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.entity.TeacherStudentNote;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;
import com.duolinfo.ia.proj.repository.TeacherStudentNoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherStudentNoteService {

    private final TeacherStudentNoteRepository noteRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Transactional
    public TeacherStudentNoteResponseDTO saveNote(Long teacherId, TeacherStudentNoteRequestDTO dto) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));

        Student student = studentRepository.findById(dto.studentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        SchoolClass schoolClass = null;
        if (dto.schoolClassId() != null) {
            schoolClass = schoolClassRepository.findById(dto.schoolClassId()).orElse(null);
        }

        TeacherStudentNote note = TeacherStudentNote.builder()
            .teacher(teacher)
            .student(student)
            .schoolClass(schoolClass)
            .note(dto.note().trim())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        note = noteRepository.save(note);
        return mapToDTO(note);
    }

    @Transactional(readOnly = true)
    public List<TeacherStudentNoteResponseDTO> getNotes(Long teacherId, Long studentId, Long classId) {
        List<TeacherStudentNote> list;
        if (studentId != null) {
            list = noteRepository.findByTeacher_IdAndStudent_IdOrderByUpdatedAtDesc(teacherId, studentId);
        } else if (classId != null) {
            list = noteRepository.findByTeacher_IdAndSchoolClass_IdOrderByUpdatedAtDesc(teacherId, classId);
        } else {
            list = List.of();
        }

        return list.stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public void deleteNote(Long teacherId, Long noteId) {
        TeacherStudentNote note = noteRepository.findByIdAndTeacher_Id(noteId, teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anotação não encontrada ou você não tem permissão para excluí-la."));

        noteRepository.delete(note);
    }

    private TeacherStudentNoteResponseDTO mapToDTO(TeacherStudentNote n) {
        return new TeacherStudentNoteResponseDTO(
            n.getId(),
            n.getTeacher().getId(),
            n.getTeacher().getName(),
            n.getStudent().getId(),
            n.getStudent().getName(),
            n.getSchoolClass() != null ? n.getSchoolClass().getId() : null,
            n.getSchoolClass() != null ? n.getSchoolClass().getName() : null,
            n.getNote(),
            n.getCreatedAt(),
            n.getUpdatedAt()
        );
    }
}
