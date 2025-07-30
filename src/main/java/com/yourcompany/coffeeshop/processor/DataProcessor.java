package com.yourcompany.coffeeshop.processor;

import com.yourcompany.coffeeshop.model.MenuItem;
import com.yourcompany.coffeeshop.model.SaleRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DataProcessor.class);

    // Мапа для нормалізації назв товарів (сира назва -> стандартизована назва -> MenuItem)
    private final Map<String, MenuItem> menuItemLookupMap;

    public DataProcessor(List<MenuItem> menuItems) {
        // Створення мапи для швидкого пошуку стандартизованих пунктів меню
        // Ми нормалізуємо ключі, щоб полегшити порівняння
        this.menuItemLookupMap = menuItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getNameStandard().toLowerCase().trim(),
                        item -> item
                ));
        logger.info("Initialized DataProcessor with {} menu items.", menuItemLookupMap.size());
    }

    // Метод для очищення та трансформації записів про продажі
    public List<SaleRecord> processSalesData(List<SaleRecord> rawSales) {
        List<SaleRecord> processedSales = rawSales.stream()
                .filter(this::isValidSaleRecord) // Фільтрація невалідних записів
                .map(this::transformSaleRecord) // Трансформація кожного запису
                .collect(Collectors.toList());

        logger.info("Processed {} raw sales into {} valid records.", rawSales.size(), processedSales.size());
        return processedSales;
    }

    // --- Логіка очищення та валідації ---
    private boolean isValidSaleRecord(SaleRecord record) {
        // Перевірка на NULL значення, які могли залишитися або бути встановлені під час читання
        if (record.getDate() == null || record.getItemNameRaw() == null || record.getItemNameRaw().trim().isEmpty() ||
                record.getPaymentMethod() == null || record.getBaristaName() == null ||
                record.getPricePerItem() == null || record.getPricePerItem().compareTo(BigDecimal.ZERO) < 0 ||
                record.getQuantity() <= 0) {
            logger.warn("Invalid record found (missing fields or negative values): {}", record);
            return false;
        }
        return true;
    }

    // --- Логіка трансформації ---
    private SaleRecord transformSaleRecord(SaleRecord record) {
        // 1. Нормалізація item_name:
        String cleanedItemNameRaw = record.getItemNameRaw().toLowerCase().trim();
        MenuItem matchedMenuItem = menuItemLookupMap.get(cleanedItemNameRaw);

        // Спробуємо знайти найближчий варіант, якщо точного співпадіння немає
        // Це може бути більш складна логіка, наприклад, з використанням Levenshtein distance
        if (matchedMenuItem == null) {
            // Якщо не знайдено точного співпадіння, спробуємо знайти за частковим збігом або ручними правилами
            // Наприклад, "латте" -> "Лате"
            if (cleanedItemNameRaw.contains("латте") || cleanedItemNameRaw.contains("latte")) {
                matchedMenuItem = menuItemLookupMap.get("лате"); // Припускаємо, що "лате" є стандартом
            } else if (cleanedItemNameRaw.contains("капучино") || cleanedItemNameRaw.contains("cappuccino")) {
                matchedMenuItem = menuItemLookupMap.get("капучино");
            }
            // ... додайте інші правила нормалізації
        }

        if (matchedMenuItem != null) {
            record.setItemNameStandard(matchedMenuItem.getNameStandard());
        } else {
            // Якщо не вдалося стандартизувати, можна залишити сиру назву або позначити як "Unknown"
            record.setItemNameStandard("Unknown / " + record.getItemNameRaw());
            logger.warn("Could not standardize item name '{}' for sale_id: {}", record.getItemNameRaw(), record.getSaleId());
            // Можна також задати default values для pricePerItem та costPerUnit, якщо вони залежать від matchedMenuItem
        }

        // 2. Розрахунок total_sale_price:
        // Використовуйте BigDecimal для точних грошових розрахунків
        BigDecimal totalSalePrice = record.getPricePerItem().multiply(BigDecimal.valueOf(record.getQuantity()));
        record.setTotalSalePrice(totalSalePrice.setScale(2, RoundingMode.HALF_UP)); // Округлити до 2 знаків після коми

        // 3. Розрахунок profit_per_item та total_profit
        if (matchedMenuItem != null && matchedMenuItem.getCostPerUnit() != null) {
            BigDecimal profitPerUnit = record.getPricePerItem().subtract(matchedMenuItem.getCostPerUnit());
            record.setProfitPerItem(profitPerUnit.setScale(2, RoundingMode.HALF_UP));

            BigDecimal totalProfit = profitPerUnit.multiply(BigDecimal.valueOf(record.getQuantity()));
            record.setTotalProfit(totalProfit.setScale(2, RoundingMode.HALF_UP));
        } else {
            record.setProfitPerItem(BigDecimal.ZERO); // Якщо собівартість невідома
            record.setTotalProfit(BigDecimal.ZERO);
            logger.warn("Cannot calculate profit for sale_id {} due to unknown cost per unit.", record.getSaleId());
        }

        // 4. Додаткова очистка: видалення зайвих пробілів, приведення до єдиного регістру
        record.setPaymentMethod(record.getPaymentMethod().trim());
        record.setBaristaName(record.getBaristaName().trim());

        return record;
    }

    // Метод для обробки дублікатів (зазвичай краще робити в SQL при вставці або через унікальні обмеження)
    // Але можна зробити і тут:
    public List<SaleRecord> removeDuplicateSales(List<SaleRecord> sales) {
        Set<Integer> uniqueSaleIds = sales.stream()
                .map(SaleRecord::getSaleId)
                .collect(Collectors.toSet());

        if (uniqueSaleIds.size() < sales.size()) {
            logger.warn("Detected {} duplicate sales records. Removing them...", sales.size() - uniqueSaleIds.size());
            // Просто повертаємо список унікальних за ID
            return sales.stream()
                    .distinct() // Вимагає реалізації equals() і hashCode() у SaleRecord!
                    .collect(Collectors.toList());
        }
        return sales;
    }

    // Щоб .distinct() працював коректно для SaleRecord, додайте до SaleRecord.java
    /*
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaleRecord that = (SaleRecord) o;
        return saleId == that.saleId; // Порівнюємо за унікальним ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId);
    }
    */
}