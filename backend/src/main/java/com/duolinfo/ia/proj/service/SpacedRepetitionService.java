package com.duolinfo.ia.proj.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.srs.SpacedReviewAnswerDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewItemRequestDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewItemResponseDTO;
import com.duolinfo.ia.proj.dto.srs.SpacedReviewSessionSummaryDTO;
import com.duolinfo.ia.proj.entity.SpacedReviewItem;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentLanguage;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.repository.SpacedReviewItemRepository;
import com.duolinfo.ia.proj.repository.StudentRepository;

@Service
public class SpacedRepetitionService {

    private final SpacedReviewItemRepository itemRepository;
    private final StudentRepository studentRepository;

    @Autowired(required = false)
    private SseEmitterService sseEmitterService;

    public SpacedRepetitionService(
            SpacedReviewItemRepository itemRepository,
            StudentRepository studentRepository) {
        this.itemRepository = itemRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public List<SpacedReviewItemResponseDTO> getDueSession(String email, String languageName) {
        Student student = requireStudent(email);
        LocalDate today = LocalDate.now();

        List<SpacedReviewItem> dueItems = (languageName != null && !languageName.isBlank())
            ? itemRepository.findDueItemsByLanguage(student.getId(), languageName.trim(), today)
            : itemRepository.findDueItems(student.getId(), today);

        if (dueItems.isEmpty()) {
            // Seed starter vocabulary if completely empty for this student
            long total = itemRepository.countByStudent_Id(student.getId());
            if (total == 0) {
                seedStarterVocabulary(student, languageName != null && !languageName.isBlank() ? languageName.trim() : "Inglês");
                dueItems = (languageName != null && !languageName.isBlank())
                    ? itemRepository.findDueItemsByLanguage(student.getId(), languageName.trim(), today)
                    : itemRepository.findDueItems(student.getId(), today);
            }
        }

        return dueItems.stream()
            .map(this::toResponseDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SpacedReviewItemResponseDTO> listItems(String email, String languageName) {
        Student student = requireStudent(email);
        List<SpacedReviewItem> list = (languageName != null && !languageName.isBlank())
            ? itemRepository.findByStudent_IdAndLanguageNameIgnoreCaseOrderByDueDateAsc(student.getId(), languageName.trim())
            : itemRepository.findByStudent_IdOrderByDueDateAsc(student.getId());

        return list.stream()
            .map(this::toResponseDTO)
            .toList();
    }

    @Transactional
    public SpacedReviewItemResponseDTO createItem(String email, SpacedReviewItemRequestDTO request) {
        Student student = requireStudent(email);

        SpacedReviewItem item = SpacedReviewItem.builder()
            .student(student)
            .languageName(request.languageName().trim())
            .term(request.term().trim())
            .translation(request.translation().trim())
            .exampleSentence(clean(request.exampleSentence()))
            .exampleTranslation(clean(request.exampleTranslation()))
            .proficiencyLevel(request.proficiencyLevel() != null && !request.proficiencyLevel().isBlank() ? request.proficiencyLevel().trim() : "A1")
            .tags(clean(request.tags()))
            .repetitionNumber(0)
            .intervalDays(1)
            .easeFactor(2.5)
            .dueDate(LocalDate.now())
            .totalReviews(0)
            .lapseCount(0)
            .xpEarned(0)
            .build();

        SpacedReviewItem saved = itemRepository.save(item);
        return toResponseDTO(saved);
    }

    @Transactional
    public SpacedReviewItemResponseDTO reviewItem(String email, Long itemId, SpacedReviewAnswerDTO answer) {
        Student student = requireStudent(email);
        SpacedReviewItem item = itemRepository.findByIdAndStudent_Id(itemId, student.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Item de vocabulário não encontrado."));

        int rating = answer.rating(); // 1=Again, 2=Hard, 3=Good, 4=Easy
        int repetition = item.getRepetitionNumber();
        int interval = item.getIntervalDays();
        double ease = item.getEaseFactor();
        int xpEarned;

        // SuperMemo SM-2 Algorithm Implementation
        if (rating < 3) {
            // Failure / Lapse
            repetition = 0;
            interval = 1;
            item.setLapseCount(item.getLapseCount() + 1);
            ease = Math.max(1.30, ease - 0.20);
            xpEarned = 2; // consolation attempt XP
        } else {
            // Success (Good / Easy)
            if (repetition == 0) {
                interval = 1;
            } else if (repetition == 1) {
                interval = (rating == 4) ? 6 : 4;
            } else {
                double bonus = (rating == 4) ? 1.3 : 1.0;
                interval = Math.max(1, (int) Math.round(interval * ease * bonus));
            }
            repetition++;
            ease = Math.max(1.30, ease + (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02)));
            xpEarned = (rating == 4) ? 10 : 6;
        }

        LocalDate nextDueDate = LocalDate.now().plusDays(interval);

        item.setRepetitionNumber(repetition);
        item.setIntervalDays(interval);
        item.setEaseFactor(ease);
        item.setDueDate(nextDueDate);
        item.setLastReviewedAt(Instant.now());
        item.setTotalReviews(item.getTotalReviews() + 1);
        item.setXpEarned(item.getXpEarned() + xpEarned);

        SpacedReviewItem saved = itemRepository.save(item);

        // Award XP to student's language score
        awardXpToStudentLanguage(student, item.getLanguageName(), xpEarned);

        if (sseEmitterService != null) {
            sseEmitterService.sendToUser(student.getId(), "SRS_REVIEW_COMPLETED", java.util.Map.of(
                "itemId", saved.getId(),
                "nextDueDate", saved.getDueDate().toString(),
                "xpEarned", xpEarned,
                "intervalDays", saved.getIntervalDays()
            ));
        }

        return toResponseDTO(saved);
    }

    @Transactional
    public void deleteItem(String email, Long itemId) {
        Student student = requireStudent(email);
        SpacedReviewItem item = itemRepository.findByIdAndStudent_Id(itemId, student.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Item de vocabulário não encontrado."));
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public SpacedReviewSessionSummaryDTO getStats(String email) {
        Student student = requireStudent(email);
        LocalDate today = LocalDate.now();

        List<SpacedReviewItem> all = itemRepository.findByStudent_IdOrderByDueDateAsc(student.getId());
        long totalCards = all.size();
        long dueToday = all.stream().filter(i -> !i.getDueDate().isAfter(today)).count();
        long completedToday = all.stream().filter(i -> i.getLastReviewedAt() != null
                && i.getLastReviewedAt().isAfter(today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())).count();
        int totalXp = all.stream().mapToInt(SpacedReviewItem::getXpEarned).sum();
        int totalReviews = all.stream().mapToInt(SpacedReviewItem::getTotalReviews).sum();
        int totalLapses = all.stream().mapToInt(SpacedReviewItem::getLapseCount).sum();

        double retentionRate = totalReviews == 0 ? 100.0 : Math.max(0.0, Math.min(100.0,
            ((double) (totalReviews - totalLapses) / totalReviews) * 100.0));

        int mastered = (int) all.stream().filter(i -> i.getRepetitionNumber() >= 4 && i.getIntervalDays() >= 21).count();
        int learning = (int) (totalCards - mastered);

        return new SpacedReviewSessionSummaryDTO(
            totalCards,
            dueToday,
            completedToday,
            totalXp,
            Math.round(retentionRate * 10.0) / 10.0,
            mastered,
            learning
        );
    }

    private void awardXpToStudentLanguage(Student student, String languageName, int xp) {
        if (student.getLanguages() == null || student.getLanguages().isEmpty()) {
            return;
        }
        for (StudentLanguage sl : student.getLanguages()) {
            if (sl.getLanguageName() != null && sl.getLanguageName().trim().equalsIgnoreCase(languageName.trim())) {
                sl.setScore((sl.getScore() != null ? sl.getScore() : 0) + xp);
                break;
            }
        }
        studentRepository.save(student);
    }

    private void seedStarterVocabulary(Student student, String languageName) {
        String lang = (languageName != null && !languageName.isBlank()) ? languageName.trim() : "Inglês";
        List<SpacedReviewItem> starter = new ArrayList<>();

        if ("Inglês".equalsIgnoreCase(lang) || "English".equalsIgnoreCase(lang)) {
            starter.add(buildItem(student, lang, "overwhelmed", "sobrecarregado(a)", "She felt overwhelmed by the amount of work.", "Ela se sentiu sobrecarregada com a quantidade de trabalho.", "B1", "adjetivo"));
            starter.add(buildItem(student, lang, "reliable", "confiável", "He is a reliable friend who never lets you down.", "Ele é um amigo confiável que nunca te desaponta.", "A2", "adjetivo"));
            starter.add(buildItem(student, lang, "to look forward to", "esperar ansiosamente por", "I look forward to meeting you tomorrow.", "Estou ansioso para encontrá-lo amanhã.", "B1", "phrasal verb"));
            starter.add(buildItem(student, lang, "straightforward", "direto, simples, descomplicado", "The instructions were clear and straightforward.", "As instruções eram claras e diretas.", "B2", "adjetivo"));
            starter.add(buildItem(student, lang, "seldom", "raramente", "They seldom eat out during weekdays.", "Eles raramente comem fora durante a semana.", "B1", "advérbio"));
        } else if ("Espanhol".equalsIgnoreCase(lang) || "Spanish".equalsIgnoreCase(lang)) {
            starter.add(buildItem(student, lang, "desarrollo", "desenvolvimento", "El desarrollo de la tecnología es impresionante.", "O desenvolvimento da tecnologia é impressionante.", "A2", "substantivo"));
            starter.add(buildItem(student, lang, "sin embargo", "no entanto, porém", "Estudió mucho, sin embargo no aprobó.", "Estudou muito, porém não passou.", "B1", "conector"));
            starter.add(buildItem(student, lang, "echar de menos", "sentir saudades", "Te echo mucho de menos.", "Sinto muito a sua falta.", "A2", "expressão"));
            starter.add(buildItem(student, lang, "cotidiano", "do dia a dia, cotidiano", "Es parte de nuestra vida cotidiana.", "Faz parte da nossa vida cotidiana.", "B1", "adjetivo"));
            starter.add(buildItem(student, lang, "acontecer", "acontecer, ocorrer", "¿Qué va a acontecer ahora?", "O que vai acontecer agora?", "B1", "verbo"));
        } else if ("Francês".equalsIgnoreCase(lang) || "French".equalsIgnoreCase(lang)) {
            starter.add(buildItem(student, lang, "cependant", "no entanto, porém", "Il fait froid, cependant le soleil brille.", "Está frio, no entanto o sol brilha.", "B1", "conector"));
            starter.add(buildItem(student, lang, "quotidien", "cotidiano", "Dans la vie quotidienne, c'est utile.", "Na vida cotidiana, isso é útil.", "A2", "adjetivo"));
            starter.add(buildItem(student, lang, "avoir hâte de", "estar ansioso para", "J'ai hâte de te revoir.", "Estou ansioso para te rever.", "B1", "expressão"));
        } else {
            starter.add(buildItem(student, lang, "greetings", "saudações", "Greetings from Brazil!", "Saudações do Brasil!", "A1", "geral"));
            starter.add(buildItem(student, lang, "challenge", "desafio", "A new language is an exciting challenge.", "Um novo idioma é um desafio emocionante.", "A2", "substantivo"));
            starter.add(buildItem(student, lang, "progress", "progresso", "Daily practice leads to steady progress.", "A prática diária leva a um progresso constante.", "A2", "substantivo"));
        }

        itemRepository.saveAll(starter);
    }

    private SpacedReviewItem buildItem(Student student, String lang, String term, String translation,
                                      String example, String exampleTr, String level, String tags) {
        return SpacedReviewItem.builder()
            .student(student)
            .languageName(lang)
            .term(term)
            .translation(translation)
            .exampleSentence(example)
            .exampleTranslation(exampleTr)
            .proficiencyLevel(level)
            .tags(tags)
            .repetitionNumber(0)
            .intervalDays(1)
            .easeFactor(2.5)
            .dueDate(LocalDate.now())
            .totalReviews(0)
            .lapseCount(0)
            .xpEarned(0)
            .build();
    }

    private SpacedReviewItemResponseDTO toResponseDTO(SpacedReviewItem item) {
        boolean isDue = !item.getDueDate().isAfter(LocalDate.now());
        return new SpacedReviewItemResponseDTO(
            item.getId(),
            item.getLanguageName(),
            item.getTerm(),
            item.getTranslation(),
            item.getExampleSentence(),
            item.getExampleTranslation(),
            item.getProficiencyLevel(),
            item.getTags(),
            item.getRepetitionNumber(),
            item.getIntervalDays(),
            item.getEaseFactor(),
            item.getDueDate(),
            isDue,
            item.getLastReviewedAt(),
            item.getTotalReviews(),
            item.getLapseCount(),
            item.getXpEarned()
        );
    }

    private Student requireStudent(String email) {
        return studentRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Acesso restrito ao perfil de estudante."));
    }

    private static String clean(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
