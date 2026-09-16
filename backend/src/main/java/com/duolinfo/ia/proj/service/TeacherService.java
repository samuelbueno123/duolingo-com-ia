package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.entity.Teacher;
import com.duolinfo.ia.proj.repository.TeacherRepository;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }


    // ==========================================
    // CRUD
    // ==========================================

    public Teacher create(Teacher teacher) {
        return teacherRepository.save(teacher);
    }

    public Teacher update(Teacher teacher) {
        return teacherRepository.save(teacher);
    }

    public void delete(Teacher teacher) {
        teacherRepository.delete(teacher);
    }

    public void deleteById(Long id) {
        teacherRepository.deleteById(id);
    }

    public void deleteByGoogleId(String googleId) {
        teacherRepository.findByGoogleId(googleId).ifPresent(teacherRepository::delete);
    }

    public void deleteByEmail(String email) {
        teacherRepository.findByEmail(email).ifPresent(teacherRepository::delete);
    }


    // ==========================================
    // Busca
    // ==========================================

    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    public List<Teacher> findAllById(Iterable<Long> ids) {
        return teacherRepository.findAllById(ids);
    }

    public Teacher findById(Long id) {
        return teacherRepository.findById(id).orElse(null);
    }

    public Teacher findByGoogleId(String googleId) {
        return teacherRepository.findByGoogleId(googleId).orElse(null);
    }

    public Teacher findByEmail(String email) {
        return teacherRepository.findByEmail(email).orElse(null);
    }

    public List<Teacher> findByName(String name) {
        return teacherRepository.findAll().stream()
                .filter(teacher -> teacher.getName() != null && teacher.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    public List<Teacher> findByInstitution(String institution) {
        return teacherRepository.findByInstitutionContainingIgnoreCase(institution);
    }

    public List<Teacher> findByTaughtLanguage(String language) {
        return teacherRepository.findByTaughtLanguagesContainingIgnoreCase(language);
    }

    public List<Teacher> findBySpecializationArea(String specializationArea) {
        return teacherRepository.findBySpecializationAreasContainingIgnoreCase(specializationArea);
    }


    // ==========================================
    // Verificações de existência
    // ==========================================

    public boolean existsById(Long id) {
        return teacherRepository.existsById(id);
    }

    public boolean existsByGoogleId(String googleId) {
        return teacherRepository.findByGoogleId(googleId).isPresent();
    }

    public boolean existsByEmail(String email) {
        return teacherRepository.findByEmail(email).isPresent();
    }

    public boolean existsByName(String name) {
        return teacherRepository.findAll().stream()
                .anyMatch(teacher -> teacher.getName() != null && teacher.getName().equalsIgnoreCase(name));
    }
}
