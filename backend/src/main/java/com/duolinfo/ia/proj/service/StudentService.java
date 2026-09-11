package com.duolinfo.ia.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.entity.Student;
import com.duolinfo.ia.proj.repository.StudentRepository;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // ==========================================
    // CRUD
    // ==========================================

    public Student create(Student student) {
        return studentRepository.save(student);
    }

    public Student update(Student student) {
        return studentRepository.save(student);
    }

    public void delete(Student student) {
        studentRepository.delete(student);
    }

    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }

    public void deleteByGoogleId(String googleId) {
        studentRepository.findByGoogleId(googleId).ifPresent(studentRepository::delete);
    }

    public void deleteByEmail(String email) {
        studentRepository.findByEmail(email).ifPresent(studentRepository::delete);
    }

    // ==========================================
    // Busca
    // ==========================================

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public List<Student> findAllById(Iterable<Long> ids) {
        return studentRepository.findAllById(ids);
    }

    public Student findById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }

    public Student findByGoogleId(String googleId) {
        return studentRepository.findByGoogleId(googleId).orElse(null);
    }

    public Student findByEmail(String email) {
        return studentRepository.findByEmail(email).orElse(null);
    }

    public List<Student> findByName(String name) {
        return studentRepository.findAll().stream()
                .filter(student -> student.getName() != null && student.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    public List<Student> findByPreferredLanguage(String preferredLanguage) {
        return studentRepository.findByPreferredLanguage(preferredLanguage);
    }

    // ==========================================
    // Verificação
    // ==========================================

    public boolean existsById(Long id) {
        return studentRepository.existsById(id);
    }

    public boolean existsByGoogleId(String googleId) {
        return studentRepository.findByGoogleId(googleId).isPresent();
    }

    public boolean existsByEmail(String email) {
        return studentRepository.findByEmail(email).isPresent();
    }

    public boolean existsByName(String name) {
        return studentRepository.findAll().stream()
                .anyMatch(student -> student.getName() != null && student.getName().equalsIgnoreCase(name));
    }
}
