package com.duolinfo.ia.proj.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.dto.request.AdminRoleRequestDTO;
import com.duolinfo.ia.proj.dto.request.AdminRowUpdateRequestDTO;
import com.duolinfo.ia.proj.dto.request.AdminUserRequestDTO;
import com.duolinfo.ia.proj.dto.request.InstitutionAdminRoleRequestDTO;
import com.duolinfo.ia.proj.dto.request.InstitutionRequestDecisionDTO;
import com.duolinfo.ia.proj.dto.request.TeacherRequestDecisionDTO;
import com.duolinfo.ia.proj.dto.response.AdminNotificationResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminTableDataResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminTableMetadataResponseDTO;
import com.duolinfo.ia.proj.dto.response.AdminUserResponseDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionAdminDashboardDTO;
import com.duolinfo.ia.proj.dto.response.InstitutionRequestResponseDTO;
import com.duolinfo.ia.proj.dto.response.StudentAchievementsOverviewDTO;
import com.duolinfo.ia.proj.dto.response.TeacherResponseDTO;
import com.duolinfo.ia.proj.entity.InstitutionRequest.Status;
import com.duolinfo.ia.proj.service.AchievementService;
import com.duolinfo.ia.proj.service.AdminService;
import com.duolinfo.ia.proj.service.InstitutionRequestService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final InstitutionRequestService institutionRequestService;
    private final AchievementService achievementService;

    public AdminController(
            AdminService adminService,
            InstitutionRequestService institutionRequestService,
            AchievementService achievementService) {
        this.adminService = adminService;
        this.institutionRequestService = institutionRequestService;
        this.achievementService = achievementService;
    }

    @GetMapping("/tables")
    public List<AdminTableMetadataResponseDTO> listTables(Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return adminService.listTables();
    }

    @GetMapping("/tables/{tableName}/rows")
    public AdminTableDataResponseDTO getRows(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return adminService.getTableRows(tableName, limit, offset, search);
    }

    @PostMapping("/tables/{tableName}/rows")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> insertRow(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> values,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return adminService.insertRow(tableName, values);
    }

    @PutMapping("/tables/{tableName}/rows")
    public Map<String, Object> updateRow(
            @PathVariable String tableName,
            @RequestBody AdminRowUpdateRequestDTO request,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return adminService.updateRow(tableName, request.keys(), request.values());
    }

    @DeleteMapping("/tables/{tableName}/rows")
    public Map<String, Object> deleteRow(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> keys,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return adminService.deleteRow(tableName, keys);
    }

    @GetMapping("/users")
    public List<AdminUserResponseDTO> listUsers(Authentication authentication) {
        return adminService.listUsers(authentication.getName());
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponseDTO createUser(
            @Valid @RequestBody AdminUserRequestDTO request,
            Authentication authentication) {
        return adminService.createUser(request, authentication.getName());
    }

    @PutMapping("/users/{id}")
    public AdminUserResponseDTO updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserRequestDTO request,
            Authentication authentication) {
        return adminService.updateUser(id, request, authentication.getName());
    }

    @PatchMapping("/users/{id}/admin")
    public AdminUserResponseDTO setAdminRole(
            @PathVariable Long id,
            @RequestBody AdminRoleRequestDTO request,
            Authentication authentication) {
        return adminService.setAdminRole(id, request.isAdmin(), authentication.getName());
    }

    @PatchMapping("/teachers/{id}/institution-admin")
    public TeacherResponseDTO setInstitutionAdminRole(
            @PathVariable Long id,
            @RequestBody InstitutionAdminRoleRequestDTO request,
            Authentication authentication) {
        return adminService.setInstitutionAdminRole(id, request, authentication.getName());
    }

    @GetMapping("/institution-dashboard")
    public InstitutionAdminDashboardDTO getInstitutionDashboard(
            @RequestParam(required = false) Long institutionId,
            Authentication authentication) {
        return adminService.getInstitutionDashboard(institutionId, authentication.getName());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, Authentication authentication) {
        adminService.deleteUser(id, authentication.getName());
    }

    @GetMapping("/notifications")
    public List<AdminNotificationResponseDTO> notifications(Authentication authentication) {
        return institutionRequestService.notifications(authentication.getName());
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unreadNotificationsCount(Authentication authentication) {
        return Map.of("unreadCount", institutionRequestService.unreadCount(authentication.getName()));
    }

    @PatchMapping("/notifications/{id}/read")
    public AdminNotificationResponseDTO markNotificationRead(@PathVariable Long id) {
        return institutionRequestService.markRead(id);
    }

    @GetMapping("/institution-requests")
    public List<InstitutionRequestResponseDTO> institutionRequests(
            @RequestParam(required = false) Status status) {
        return institutionRequestService.requests(status);
    }

    @PatchMapping("/institution-requests/{id}")
    public InstitutionRequestResponseDTO decideInstitutionRequest(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionRequestDecisionDTO request) {
        return institutionRequestService.decide(id, request);
    }

    @PatchMapping("/teacher-requests/{teacherId}")
    public TeacherResponseDTO decideTeacherRequest(
            @PathVariable Long teacherId,
            @Valid @RequestBody TeacherRequestDecisionDTO request,
            Authentication authentication) {
        return institutionRequestService.decideTeacherRequest(teacherId, request, authentication.getName());
    }

    @GetMapping("/students/achievements")
    public List<StudentAchievementsOverviewDTO> searchStudentsWithAchievements(
            @RequestParam(required = false) String search,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return achievementService.searchStudentsWithAchievements(search);
    }

    @GetMapping("/students/{studentId}/achievements")
    public StudentAchievementsOverviewDTO getStudentAchievements(
            @PathVariable Long studentId,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return achievementService.getStudentOverview(studentId);
    }

    @PostMapping("/students/{studentId}/achievements/{achievementId}")
    public StudentAchievementsOverviewDTO grantStudentAchievement(
            @PathVariable Long studentId,
            @PathVariable Long achievementId,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return achievementService.grantAchievement(studentId, achievementId, authentication.getName());
    }

    @DeleteMapping("/students/{studentId}/achievements/{achievementId}")
    public StudentAchievementsOverviewDTO revokeStudentAchievement(
            @PathVariable Long studentId,
            @PathVariable Long achievementId,
            Authentication authentication) {
        adminService.requireSuperAdmin(authentication.getName());
        return achievementService.revokeAchievement(studentId, achievementId);
    }
}

