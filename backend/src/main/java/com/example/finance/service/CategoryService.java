package com.example.finance.service;

import com.example.finance.dto.category.CategoryRequest;
import com.example.finance.dto.category.CategoryResponse;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryType;
import com.example.finance.entity.User;
import com.example.finance.exception.BadRequestException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        User user = userService.getCurrentAuthenticatedUser();

        if (categoryRepository.existsByNameAndUserAndType(request.getName(), user, request.getType())) {
            throw new BadRequestException("Category '" + request.getName() + "' already exists for type " + request.getType());
        }

        Category category = new Category(user, request.getName(), request.getType());
        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(CategoryType type) {
        User user = userService.getCurrentAuthenticatedUser();
        List<Category> categories;
        if (type != null) {
            categories = categoryRepository.findByUserAndType(user, type);
        } else {
            categories = categoryRepository.findByUser(user);
        }
        return categories.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        User user = userService.getCurrentAuthenticatedUser();
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        category.setName(request.getName());
        category.setType(request.getType());

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        User user = userService.getCurrentAuthenticatedUser();
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        categoryRepository.delete(category);
    }

    public CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt()
        );
    }
}
