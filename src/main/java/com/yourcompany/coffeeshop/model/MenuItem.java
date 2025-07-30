package com.yourcompany.coffeeshop.model;

import java.math.BigDecimal;

public class MenuItem {
    private int itemId;
    private String nameStandard;
    private String category;
    private BigDecimal standardPrice;
    private BigDecimal costPerUnit;

    public MenuItem(int itemId, String nameStandard, String category, BigDecimal standardPrice, BigDecimal costPerUnit) {
        this.itemId = itemId;
        this.nameStandard = nameStandard;
        this.category = category;
        this.standardPrice = standardPrice;
        this.costPerUnit = costPerUnit;
    }

    // Геттери та Сеттери
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getNameStandard() { return nameStandard; }
    public void setNameStandard(String nameStandard) { this.nameStandard = nameStandard; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getStandardPrice() { return standardPrice; }
    public void setStandardPrice(BigDecimal standardPrice) { this.standardPrice = standardPrice; }
    public BigDecimal getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(BigDecimal costPerUnit) { this.costPerUnit = costPerUnit; }

    @Override
    public String toString() {
        return "MenuItem{" +
                "itemId=" + itemId +
                ", nameStandard='" + nameStandard + '\'' +
                ", category='" + category + '\'' +
                ", standardPrice=" + standardPrice +
                ", costPerUnit=" + costPerUnit +
                '}';
    }
}