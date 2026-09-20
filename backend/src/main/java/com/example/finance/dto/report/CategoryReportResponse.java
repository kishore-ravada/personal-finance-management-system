package com.example.finance.dto.report;

import java.math.BigDecimal;

public class CategoryReportResponse {

    private String categoryName;
    private String categoryType;
    private BigDecimal totalAmount;
    private double percentage;

    public CategoryReportResponse() {}

    public CategoryReportResponse(String categoryName, String categoryType, BigDecimal totalAmount, double percentage) {
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.totalAmount = totalAmount;
        this.percentage = percentage;
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryType() { return categoryType; }
    public void setCategoryType(String categoryType) { this.categoryType = categoryType; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}
