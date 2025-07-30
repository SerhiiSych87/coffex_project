package com.yourcompany.coffeeshop.model;

import java.time.LocalDate;
import java.math.BigDecimal; // Використовуйте BigDecimal для грошових значень

public class SaleRecord {
    private int saleId;
    private LocalDate date;
    private String itemNameRaw; // Початкова сира назва товару
    private int quantity;
    private BigDecimal pricePerItem;
    private String paymentMethod;
    private String baristaName;

    // Після очищення та трансформації:
    private String itemNameStandard; // Стандартизована назва товару
    private BigDecimal totalSalePrice; // Загальна ціна за цю позицію продажу
    private BigDecimal profitPerItem; // Прибуток з цієї позиції (за одиницю)
    private BigDecimal totalProfit; // Загальний прибуток з цієї позиції

    // Конструктор
    public SaleRecord(int saleId, LocalDate date, String itemNameRaw, int quantity, BigDecimal pricePerItem, String paymentMethod, String baristaName) {
        this.saleId = saleId;
        this.date = date;
        this.itemNameRaw = itemNameRaw;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
        this.paymentMethod = paymentMethod;
        this.baristaName = baristaName;
    }

    // Геттери та Сеттери (генеровані через IntelliJ IDEA: Alt+Insert -> Getter and Setter)

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getItemNameRaw() { return itemNameRaw; }
    public void setItemNameRaw(String itemNameRaw) { this.itemNameRaw = itemNameRaw; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getPricePerItem() { return pricePerItem; }
    public void setPricePerItem(BigDecimal pricePerItem) { this.pricePerItem = pricePerItem; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getBaristaName() { return baristaName; }
    public void setBaristaName(String baristaName) { this.baristaName = baristaName; }
    public String getItemNameStandard() { return itemNameStandard; }
    public void setItemNameStandard(String itemNameStandard) { this.itemNameStandard = itemNameStandard; }
    public BigDecimal getTotalSalePrice() { return totalSalePrice; }
    public void setTotalSalePrice(BigDecimal totalSalePrice) { this.totalSalePrice = totalSalePrice; }
    public BigDecimal getProfitPerItem() { return profitPerItem; }
    public void setProfitPerItem(BigDecimal profitPerItem) { this.profitPerItem = profitPerItem; }
    public BigDecimal getTotalProfit() { return totalProfit; }
    public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }

    @Override
    public String toString() {
        return "SaleRecord{" +
                "saleId=" + saleId +
                ", date=" + date +
                ", itemNameRaw='" + itemNameRaw + '\'' +
                ", quantity=" + quantity +
                ", pricePerItem=" + pricePerItem +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", baristaName='" + baristaName + '\'' +
                ", itemNameStandard='" + itemNameStandard + '\'' +
                ", totalSalePrice=" + totalSalePrice +
                ", totalProfit=" + totalProfit +
                '}';
    }
}