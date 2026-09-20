package com.example.finance.repository;

import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryType;
import com.example.finance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    List<Category> findByUserAndType(User user, CategoryType type);
    Optional<Category> findByIdAndUser(Long id, User user);
    boolean existsByNameAndUserAndType(String name, User user, CategoryType type);
}
