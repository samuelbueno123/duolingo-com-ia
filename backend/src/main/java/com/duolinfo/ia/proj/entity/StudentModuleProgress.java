package com.duolinfo.ia.proj.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_module_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentModuleProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long moduleId;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "LOCKED"; // "LOCKED", "IN_PROGRESS", "COMPLETED"

    @Column(nullable = false)
    @Builder.Default
    private Double scorePercent = 0.0;

    private LocalDateTime completedAt;
}
