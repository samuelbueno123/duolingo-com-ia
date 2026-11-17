package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.ClassAnnouncementRequestDTO;
import com.duolinfo.ia.proj.dto.response.ClassAnnouncementResponseDTO;
import com.duolinfo.ia.proj.entity.ClassAnnouncement;
import com.duolinfo.ia.proj.entity.Enrollment;
import com.duolinfo.ia.proj.entity.EnrollmentStatus;
import com.duolinfo.ia.proj.entity.SchoolClass;
import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.ClassAnnouncementRepository;
import com.duolinfo.ia.proj.repository.EnrollmentRepository;
import com.duolinfo.ia.proj.repository.SchoolClassRepository;
import com.duolinfo.ia.proj.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassAnnouncementService {

    private final ClassAnnouncementRepository announcementRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentNotificationService studentNotificationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AccessControlService accessControlService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SseEmitterService sseEmitterService;

    @Transactional
    public ClassAnnouncementResponseDTO createAnnouncement(
            Long teacherId,
            Long classId,
            ClassAnnouncementRequestDTO dto) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));

        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));

        if (accessControlService != null && accessControlService.getCurrentUser().isPresent()) {
            accessControlService.assertCanManageSchoolClass(schoolClass);
            if (!Boolean.TRUE.equals(teacher.getApproved()) && !accessControlService.isSuperAdmin(teacher)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Professor aguardando aprovação institucional.");
            }
        }

        ClassAnnouncement announcement = ClassAnnouncement.builder()
            .schoolClass(schoolClass)
            .teacher(teacher)
            .title(dto.title().trim())
            .content(dto.content().trim())
            .priority(dto.priority() != null ? dto.priority().trim().toUpperCase() : "NORMAL")
            .pinned(Boolean.TRUE.equals(dto.pinned()))
            .createdAt(LocalDateTime.now())
            .build();

        announcement = announcementRepository.save(announcement);

        // Notify all active enrolled students in this class
        List<Enrollment> activeEnrollments = enrollmentRepository.findBySchoolClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        java.util.List<Long> studentIds = new java.util.ArrayList<>();

        for (Enrollment e : activeEnrollments) {
            if (e.getStudent() != null) {
                Long sid = e.getStudent().getId();
                studentIds.add(sid);
                studentNotificationService.notifyStudent(
                    sid,
                    "Aviso da turma " + schoolClass.getName() + ": " + announcement.getTitle(),
                    announcement.getContent(),
                    "ANNOUNCEMENT",
                    "/classes"
                );
            }
        }

        if (sseEmitterService != null && !studentIds.isEmpty()) {
            try {
                sseEmitterService.sendToUsers(studentIds, "CLASS_ANNOUNCEMENT", java.util.Map.of(
                    "announcementId", announcement.getId(),
                    "classId", schoolClass.getId(),
                    "className", schoolClass.getName(),
                    "teacherName", teacher.getName(),
                    "title", announcement.getTitle(),
                    "content", announcement.getContent(),
                    "priority", announcement.getPriority()
                ));
            } catch (Exception ignored) {
            }
        }

        return mapToDTO(announcement);
    }

    @Transactional(readOnly = true)
    public List<ClassAnnouncementResponseDTO> getAnnouncementsByClass(Long classId) {
        if (accessControlService != null && accessControlService.getCurrentUser().isPresent()) {
            accessControlService.assertCanViewSchoolClass(classId);
        }
        return announcementRepository.findBySchoolClass_IdOrderByPinnedDescCreatedAtDesc(classId)
            .stream()
            .map(this::mapToDTO)
            .toList();
    }

    @Transactional
    public void deleteAnnouncement(Long teacherId, Long announcementId) {
        ClassAnnouncement announcement = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado."));

        if (!announcement.getTeacher().getId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o autor pode excluir este comunicado.");
        }

        announcementRepository.delete(announcement);
    }

    private ClassAnnouncementResponseDTO mapToDTO(ClassAnnouncement a) {
        return new ClassAnnouncementResponseDTO(
            a.getId(),
            a.getSchoolClass().getId(),
            a.getSchoolClass().getName(),
            a.getTeacher().getId(),
            a.getTeacher().getName(),
            a.getTitle(),
            a.getContent(),
            a.getPriority(),
            a.isPinned(),
            a.getCreatedAt()
        );
    }
}
