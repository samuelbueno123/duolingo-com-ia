package com.duolinfo.ia.proj.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duolinfo.ia.proj.dto.request.VocabularyRequestDTO;
import com.duolinfo.ia.proj.dto.response.VocabularyResponseDTO;
import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.entity.StudentVocabulary;
import com.duolinfo.ia.proj.repository.StudentRepository;
import com.duolinfo.ia.proj.repository.StudentVocabularyRepository;
import com.duolinfo.ia.proj.util.SupportedLanguages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentVocabularyService {

    private final StudentVocabularyRepository vocabularyRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public VocabularyResponseDTO addWord(Long studentId, VocabularyRequestDTO dto) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudante não encontrado."));

        String lang = SupportedLanguages.normalizeOrThrow(dto.language());
        String term = dto.term().trim();

        StudentVocabulary vocab = vocabularyRepository.findByStudent_IdAndLanguageIgnoreCaseOrderByCreatedAtDesc(studentId, lang)
            .stream()
            .filter(v -> v.getTerm().equalsIgnoreCase(term))
            .findFirst()
            .orElseGet(() -> StudentVocabulary.builder()
                .student(student)
                .language(lang)
                .term(term)
                .createdAt(LocalDateTime.now())
                .timesPracticed(0)
                .build()
            );

        vocab.setTranslation(dto.translation().trim());
        vocab.setPartOfSpeech(dto.partOfSpeech() != null ? dto.partOfSpeech().trim().toUpperCase() : "PALAVRA");
        vocab.setExampleSentence(dto.exampleSentence() != null ? dto.exampleSentence().trim() : null);
        vocab.setMasteryLevel(dto.masteryLevel() != null ? dto.masteryLevel().trim().toUpperCase() : "LEARNING");

        vocab = vocabularyRepository.save(vocab);
        return mapToDTO(vocab);
    }

    @Transactional(readOnly = true)
    public List<VocabularyResponseDTO> getVocabulary(Long studentId, String language, String search, String masteryLevel) {
        List<StudentVocabulary> list;
        if (language != null && !language.isBlank()) {
            String canonicalLang = SupportedLanguages.normalizeOrThrow(language);
            list = vocabularyRepository.findByStudent_IdAndLanguageIgnoreCaseOrderByCreatedAtDesc(studentId, canonicalLang);
        } else {
            list = vocabularyRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        }

        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            list = list.stream()
                .filter(v -> v.getTerm().toLowerCase().contains(query) || v.getTranslation().toLowerCase().contains(query))
                .toList();
        }

        if (masteryLevel != null && !masteryLevel.isBlank()) {
            String target = masteryLevel.trim().toUpperCase();
            list = list.stream()
                .filter(v -> target.equalsIgnoreCase(v.getMasteryLevel()))
                .toList();
        }

        return list.stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public VocabularyResponseDTO updateMastery(Long studentId, Long vocabId, String masteryLevel) {
        StudentVocabulary vocab = vocabularyRepository.findByIdAndStudent_Id(vocabId, studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Palavra não encontrada no vocabulário."));

        if (masteryLevel != null && !masteryLevel.isBlank()) {
            vocab.setMasteryLevel(masteryLevel.trim().toUpperCase());
        }
        vocab.setTimesPracticed(vocab.getTimesPracticed() + 1);

        vocab = vocabularyRepository.save(vocab);
        return mapToDTO(vocab);
    }

    @Transactional
    public void deleteWord(Long studentId, Long vocabId) {
        StudentVocabulary vocab = vocabularyRepository.findByIdAndStudent_Id(vocabId, studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Palavra não encontrada no vocabulário."));

        vocabularyRepository.delete(vocab);
    }

    private VocabularyResponseDTO mapToDTO(StudentVocabulary v) {
        return new VocabularyResponseDTO(
            v.getId(),
            v.getLanguage(),
            v.getTerm(),
            v.getTranslation(),
            v.getPartOfSpeech(),
            v.getExampleSentence(),
            v.getMasteryLevel(),
            v.getTimesPracticed(),
            v.getCreatedAt()
        );
    }
}
