package com.duolinfo.ia.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duolinfo.ia.proj.entity.LearningPathModule;

@Repository
public interface LearningPathModuleRepository extends JpaRepository<LearningPathModule, Long> {
    List<LearningPathModule> findByLanguageIgnoreCaseOrderByModuleOrderAsc(String language);
    List<LearningPathModule> findByLanguageIgnoreCaseAndCefrLevelIgnoreCaseOrderByModuleOrderAsc(String language, String cefrLevel);
}
