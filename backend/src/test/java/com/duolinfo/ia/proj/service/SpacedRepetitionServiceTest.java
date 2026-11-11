package com.duolinfo.ia.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.duolinfo.ia.proj.dto.srs.SpacedReviewAnswerDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewItemResponseDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewSessionSummaryDTO;
import com.duolinfo.ia.proj.entity.SpacedReviewItem;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.exception.BusinessException;
import com.duolinfo.ia.proj.repository.SpacedReviewItemRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

class SpacedRepetitionServiceTest {

    private SpacedReviewItemRepository itemRepository;
    private StudentRepository studentRepository;
    private SpacedRepetitionService srsService;

    @BeforeEach
    void setUp() {
        itemRepository = mock(SpacedReviewItemRepository.class);
        studentRepository = mock(StudentRepository.class);
        srsService = new SpacedRepetitionService(itemRepository, studentRepository);
    }

    @Test
    void reviewItem_rejectsRatingOutOfRange() {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@example.com");

        SpacedReviewItem item = SpacedReviewItem.builder()
            .id(10L)
            .student(student)
            .languageName("Inglês")
            .build();

        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(itemRepository.findByIdAndStudent_Id(10L, 1L)).thenReturn(Optional.of(item));

        // Negative rating
        assertThrows(
            BusinessException.class,
            () -> srsService.reviewItem("student@example.com", 10L, new SpacedReviewAnswerDTO(-1))
        );

        // Rating > 5
        assertThrows(
            BusinessException.class,
            () -> srsService.reviewItem("student@example.com", 10L, new SpacedReviewAnswerDTO(6))
        );

        // Null answer
        assertThrows(
            BusinessException.class,
            () -> srsService.reviewItem("student@example.com", 10L, null)
        );
    }

    @Test
    void reviewItem_failureLapsesAndResetsInterval() {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@example.com");

        SpacedReviewItem item = SpacedReviewItem.builder()
            .id(10L)
            .student(student)
            .languageName("Inglês")
            .repetitionNumber(4)
            .intervalDays(15)
            .easeFactor(2.5)
            .lapseCount(1)
            .dueDate(LocalDate.now())
            .build();

        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(itemRepository.findByIdAndStudent_Id(10L, 1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(SpacedReviewItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Rating 1 (Lapse / Hard failure)
        SpacedReviewItemResponseDTO response = srsService.reviewItem(
            "student@example.com", 10L, new SpacedReviewAnswerDTO(1)
        );

        assertEquals(0, response.repetitionNumber());
        assertEquals(1, response.intervalDays());
        assertEquals(2, response.lapseCount());
        assertEquals(LocalDate.now().plusDays(1), response.dueDate());
    }

    @Test
    void reviewItem_awardsXpAndDynamicallyCreatesLanguageIfMissing() {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@example.com");
        student.setLanguages(new ArrayList<>()); // Has NO languages registered yet!

        SpacedReviewItem item = SpacedReviewItem.builder()
            .id(20L)
            .student(student)
            .languageName("Italiano")
            .repetitionNumber(0)
            .intervalDays(1)
            .easeFactor(2.5)
            .lapseCount(0)
            .dueDate(LocalDate.now())
            .build();

        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(itemRepository.findByIdAndStudent_Id(20L, 1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(SpacedReviewItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Perfect review (rating 4)
        SpacedReviewItemResponseDTO response = srsService.reviewItem(
            "student@example.com", 20L, new SpacedReviewAnswerDTO(4)
        );

        assertEquals(10, response.xpEarned());
        // Verify language was created dynamically on student
        assertEquals(1, student.getLanguages().size());
        assertEquals("Italiano", student.getLanguages().get(0).getLanguageName());
        assertEquals(10, student.getLanguages().get(0).getScore());
        verify(studentRepository).save(student);
    }

    @Test
    void calculateSessionSummary_computesRetentionRateCorrectly() {
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@example.com");

        SpacedReviewItem item1 = SpacedReviewItem.builder()
            .id(1L)
            .student(student)
            .repetitionNumber(4)
            .intervalDays(25)
            .totalReviews(10)
            .lapseCount(1)
            .xpEarned(50)
            .dueDate(LocalDate.now().minusDays(1))
            .build();

        SpacedReviewItem item2 = SpacedReviewItem.builder()
            .id(2L)
            .student(student)
            .repetitionNumber(2)
            .intervalDays(5)
            .totalReviews(5)
            .lapseCount(1)
            .xpEarned(20)
            .dueDate(LocalDate.now().plusDays(2))
            .build();

        when(studentRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
        when(itemRepository.findByStudent_IdOrderByDueDateAsc(1L)).thenReturn(List.of(item1, item2));

        SpacedReviewSessionSummaryDTO summary = srsService.getStats("student@example.com");

        assertNotNull(summary);
        assertEquals(2, summary.totalCards());
        assertEquals(1, summary.dueToday());
        assertEquals(1, summary.masteredCount());
        assertEquals(1, summary.learningCount());
        assertEquals(70, summary.totalXpEarned());
        assertEquals(86.7, summary.retentionRate());
    }
}
