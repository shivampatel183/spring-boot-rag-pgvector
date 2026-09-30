package com.example.BotAI.Repository;

import com.example.BotAI.Entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    StudentEntity findByName(String name);
}
