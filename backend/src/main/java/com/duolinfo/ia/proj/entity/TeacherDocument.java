package com.duolinfo.ia.proj.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "teacher_documents")
public class TeacherDocument {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotBlank
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @NotNull
    @Column(name = "file_size", nullable = false)
    @Size(max = 10 * 1024 * 1024)
    private Long fileSize;

    @NotNull
    @Lob
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @PrePersist
    @PreUpdate
    public void validateDocument() {
        if (fileSize == null || fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Document size must be up to 10MB.");
        }

        String lowerType = contentType != null ? contentType.toLowerCase() : "";
        boolean valid = lowerType.equals("application/pdf")
                || lowerType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || lowerType.equals("application/msword")
                || lowerType.equals("application/octet-stream");

        if (!valid) {
            throw new IllegalArgumentException("Only PDF, DOCX, and DOC files are allowed.");
        }
    }
}
