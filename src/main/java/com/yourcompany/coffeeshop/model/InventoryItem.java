package com.yourcompany.coffeeshop.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public class InventoryItem {
    private int ingredientId;
    private String ingredientName;
    private BigDecimal currentStockKgL;
    private BigDecimal unitCost;
    private LocalDate lastRestockDate;

    public InventoryItem(int ingredientId, String ingredientName, BigDecimal currentStockKgL, BigDecimal unitCost, LocalDate lastRestockDate) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.currentStockKgL = currentStockKgL;
        this.unitCost = unitCost;
        this.lastRestockDate = lastRestockDate;
    }

    // Геттери та Сеттери
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public BigDecimal getCurrentStockKgL() { return currentStockKgL; }
    public void setCurrentStockKgL(BigDecimal currentStockKgL) { this.currentStockKgL = currentStockKgL; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public LocalDate getLastRestockDate() { return lastRestockDate; }
    public void setLastRestockDate(LocalDate lastRestockDate) { this.lastRestockDate = lastRestockDate; }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "ingredientId=" + ingredientId +
                ", ingredientName='" + ingredientName + '\'' +
                ", currentStockKgL=" + currentStockKgL +
                ", unitCost=" + unitCost +
                ", lastRestockDate=" + lastRestockDate +
                '}';
    }
}