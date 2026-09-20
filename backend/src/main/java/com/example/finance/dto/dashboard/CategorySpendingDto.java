package com.example.finance.dto.dashboard;

import java.math.BigDecimal;

public class CategorySpendingDto {
    private String categoryName;
    private BigDecimal amount;

    public CategorySpendingDto() {}

    public CategorySpendingDto(String categoryName, BigDecimal amount) {
        this.categoryName = categoryName;
        this.amount = amount;
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
