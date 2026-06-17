package com.example.backend.Moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistedWordRepository extends JpaRepository<BlacklistedWord, Integer> {
    boolean existsByWord(String word);
    void deleteByWord(String word);
}