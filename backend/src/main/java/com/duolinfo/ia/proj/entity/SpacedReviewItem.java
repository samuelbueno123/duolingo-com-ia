package com.duolinfo.ia.proj.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "spaced_review_items",
    indexes = {
        @Index(name = "idx_srs_student_due", columnList = "student_id, due_date"),
        @Index(name = "idx_srs_student_lang", columnList = "student_id, language_name")
    }
)
public class SpacedReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "language_name", nullable = false, length = 60)
    private String languageName;

    @Column(name = "term", nullable = false, length = 250)
    private String term;

    @Column(name = "translation", nullable = false, length = 250)
    private String translation;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    @Column(name = "example_translation", columnDefinition = "TEXT")
    private String exampleTranslation;

    @Column(name = "proficiency_level", length = 20)
    @Builder.Default
    private String proficiencyLevel = "A1";

    @Column(name = "tags", length = 150)
    private String tags;

    @Column(name = "repetition_number", nullable = false)
    @Builder.Default
    private int repetitionNumber = 0;

    @Column(name = "interval_days", nullable = false)
    @Builder.Default
    private int intervalDays = 1;

    @Column(name = "ease_factor", nullable = false)
    @Builder.Default
    private double easeFactor = 2.5;

    @Column(name = "due_date", nullable = false)
    @Builder.Default
    private LocalDate dueDate = LocalDate.now();

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private int totalReviews = 0;

    @Column(name = "lapse_count", nullable = false)
    @Builder.Default
    private int lapseCount = 0;

    @Column(name = "xp_earned", nullable = false)
    @Builder.Default
    private int xpEarned = 0;
}
