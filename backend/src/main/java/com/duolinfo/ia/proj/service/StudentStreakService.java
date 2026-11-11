package com.duolinfo.ia.proj.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.response.StreakResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentStreakService {

    private final StudentRepository studentRepository;

    @Transactional
    public StreakResponseDTO getStreak(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        LocalDate today = LocalDate.now();
        LocalDate lastActive = student.getLastActiveDate();

        int currentStreak = student.getCurrentStreak() != null ? student.getCurrentStreak() : 0;
        int freezes = student.getStreakFreezes() != null ? student.getStreakFreezes() : 2;

        if (lastActive != null && lastActive.isBefore(today.minusDays(1))) {
            // More than 1 day missed
            if (lastActive.equals(today.minusDays(2)) && freezes > 0) {
                // Auto-consume freeze to preserve streak
                student.setStreakFreezes(freezes - 1);
                student.setLastActiveDate(today.minusDays(1));
                studentRepository.save(student);
            } else {
                currentStreak = 0;
                student.setCurrentStreak(0);
                studentRepository.save(student);
            }
        }

        int todayXp = student.getTodayXpEarned() != null ? student.getTodayXpEarned() : 0;
        if (lastActive == null || !lastActive.equals(today)) {
            todayXp = 0;
        }

        int dailyGoal = student.getDailyXpGoal() != null ? student.getDailyXpGoal() : 50;
        boolean goalReached = todayXp >= dailyGoal;

        return new StreakResponseDTO(
            currentStreak,
            student.getLongestStreak() != null ? student.getLongestStreak() : currentStreak,
            student.getLastActiveDate(),
            student.getStreakFreezes() != null ? student.getStreakFreezes() : 2,
            todayXp,
            dailyGoal,
            goalReached
        );
    }

    @Transactional
    public StreakResponseDTO recordActivity(Long studentId, int xpEarned) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        LocalDate today = LocalDate.now();
        LocalDate lastActive = student.getLastActiveDate();

        int current = student.getCurrentStreak() != null ? student.getCurrentStreak() : 0;
        int longest = student.getLongestStreak() != null ? student.getLongestStreak() : 0;
        int freezes = student.getStreakFreezes() != null ? student.getStreakFreezes() : 2;
        int todayXp = student.getTodayXpEarned() != null ? student.getTodayXpEarned() : 0;

        if (lastActive == null) {
            current = 1;
            todayXp = xpEarned;
        } else if (lastActive.equals(today)) {
            todayXp += xpEarned;
        } else if (lastActive.equals(today.minusDays(1))) {
            current += 1;
            todayXp = xpEarned;
        } else if (lastActive.equals(today.minusDays(2)) && freezes > 0) {
            freezes -= 1;
            current += 1;
            todayXp = xpEarned;
        } else {
            current = 1;
            todayXp = xpEarned;
        }

        longest = Math.max(longest, current);

        student.setCurrentStreak(current);
        student.setLongestStreak(longest);
        student.setLastActiveDate(today);
        student.setStreakFreezes(freezes);
        student.setTodayXpEarned(todayXp);

        studentRepository.save(student);

        int dailyGoal = student.getDailyXpGoal() != null ? student.getDailyXpGoal() : 50;
        return new StreakResponseDTO(
            current,
            longest,
            today,
            freezes,
            todayXp,
            dailyGoal,
            todayXp >= dailyGoal
        );
    }

    @Transactional
    public StreakResponseDTO setDailyGoal(Long studentId, int dailyGoal) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        student.setDailyXpGoal(dailyGoal);
        studentRepository.save(student);

        return getStreak(studentId);
    }

    @Transactional
    public StreakResponseDTO buyStreakFreeze(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        int currentFreezes = student.getStreakFreezes() != null ? student.getStreakFreezes() : 0;
        if (currentFreezes >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite máximo de 5 bloqueios de ofensiva atingido.");
        }

        student.setStreakFreezes(currentFreezes + 1);
        studentRepository.save(student);

        return getStreak(studentId);
    }
}
