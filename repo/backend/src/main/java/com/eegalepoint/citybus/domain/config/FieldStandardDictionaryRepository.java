package com.eegalepoint.citybus.domain.config;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldStandardDictionaryRepository extends JpaRepository<FieldStandardDictionaryEntity, Long> {

  List<FieldStandardDictionaryEntity> findByFieldNameOrderByCanonicalValueAsc(String fieldName);

  List<FieldStandardDictionaryEntity> findAllByOrderByFieldNameAscCanonicalValueAsc();
}
