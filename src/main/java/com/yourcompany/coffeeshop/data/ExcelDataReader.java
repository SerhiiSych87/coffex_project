package com.yourcompany.coffeeshop.data;

import com.yourcompany.coffeeshop.model.InventoryItem;
import com.yourcompany.coffeeshop.model.MenuItem;
import com.yourcompany.coffeeshop.model.SaleRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelDataReader {

    private static final Logger logger = LoggerFactory.getLogger(ExcelDataReader.class);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy"); // Або "yyyy-MM-dd"

    // Метод для читання даних з Excel файлу
    private Workbook getWorkbook(String filePath) throws IOException {
        FileInputStream excelFile = new FileInputStream(new File(filePath));
        return new XSSFWorkbook(excelFile);
    }

    // --- Методи для читання окремих таблиць ---

    public List<SaleRecord> readDailySales(String filePath) {
        List<SaleRecord> sales = new ArrayList<>();
        try (Workbook workbook = getWorkbook(filePath)) {
            Sheet sheet = workbook.getSheetAt(0); // Беремо перший аркуш
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false; // Пропускаємо рядок заголовків
                    continue;
                }

                try {
                    // Валідація та обробка даних
                    int saleId = (int) getNumericCellValue(row.getCell(0), "sale_id");
                    LocalDate date = getLocalDateCellValue(row.getCell(1), "date");
                    String itemNameRaw = getStringCellValue(row.getCell(2), "item_name");
                    int quantity = (int) getNumericCellValue(row.getCell(3), "quantity");
                    BigDecimal pricePerItem = getBigDecimalCellValue(row.getCell(4), "price_per_item");
                    String paymentMethod = getStringCellValue(row.getCell(5), "payment_method");
                    String baristaName = getStringCellValue(row.getCell(6), "barista_name");

                    // Валідація: quantity та price_per_item мають бути позитивними
                    if (quantity <= 0) {
                        logger.warn("Invalid quantity (<=0) for sale_id: {}", saleId);
                        continue; // Пропускаємо рядок або обробляємо як помилку
                    }
                    if (pricePerItem == null || pricePerItem.compareTo(BigDecimal.ZERO) <= 0) {
                        logger.warn("Invalid price_per_item (<=0 or null) for sale_id: {}", saleId);
                        continue;
                    }

                    sales.add(new SaleRecord(saleId, date, itemNameRaw, quantity, pricePerItem, paymentMethod, baristaName));

                } catch (Exception e) {
                    logger.error("Error reading row {} from {}: {}", row.getRowNum(), filePath, e.getMessage());
                    // Можна додати логіку для збереження помилкових рядків для подальшого аналізу
                }
            }
        } catch (IOException e) {
            logger.error("Could not read Excel file {}: {}", filePath, e.getMessage());
        }
        return sales;
    }

    public List<MenuItem> readMenuItems(String filePath) {
        List<MenuItem> menuItems = new ArrayList<>();
        try (Workbook workbook = getWorkbook(filePath)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                try {
                    int itemId = (int) getNumericCellValue(row.getCell(0), "menu_item_id");
                    String nameStandard = getStringCellValue(row.getCell(1), "item_name_standard");
                    String category = getStringCellValue(row.getCell(2), "category");
                    BigDecimal standardPrice = getBigDecimalCellValue(row.getCell(3), "standard_price");
                    BigDecimal costPerUnit = getBigDecimalCellValue(row.getCell(4), "cost_per_unit");

                    if (nameStandard == null || nameStandard.trim().isEmpty()) {
                        logger.warn("Missing standardized item name for menu_item_id: {}", itemId);
                        continue;
                    }

                    menuItems.add(new MenuItem(itemId, nameStandard, category, standardPrice, costPerUnit));

                } catch (Exception e) {
                    logger.error("Error reading row {} from {}: {}", row.getRowNum(), filePath, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Could not read Excel file {}: {}", filePath, e.getMessage());
        }
        return menuItems;
    }

    public List<InventoryItem> readInventory(String filePath) {
        List<InventoryItem> inventoryItems = new ArrayList<>();
        try (Workbook workbook = getWorkbook(filePath)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                try {
                    int ingredientId = (int) getNumericCellValue(row.getCell(0), "ingredient_id");
                    String ingredientName = getStringCellValue(row.getCell(1), "ingredient_name");
                    BigDecimal currentStockKgL = getBigDecimalCellValue(row.getCell(2), "current_stock_kg_l");
                    BigDecimal unitCost = getBigDecimalCellValue(row.getCell(3), "unit_cost");
                    LocalDate lastRestockDate = getLocalDateCellValue(row.getCell(4), "last_restock_date");

                    if (ingredientName == null || ingredientName.trim().isEmpty()) {
                        logger.warn("Missing ingredient name for ingredient_id: {}", ingredientId);
                        continue;
                    }

                    inventoryItems.add(new InventoryItem(ingredientId, ingredientName, currentStockKgL, unitCost, lastRestockDate));

                } catch (Exception e) {
                    logger.error("Error reading row {} from {}: {}", row.getRowNum(), filePath, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Could not read Excel file {}: {}", filePath, e.getMessage());
        }
        return inventoryItems;
    }

    // --- Допоміжні методи для безпечного отримання значень комірок ---
    private String getStringCellValue(Cell cell, String columnName) {
        if (cell == null) {
            return null;
        }
        try {
            return cell.getStringCellValue().trim(); // Trim whitespace
        } catch (IllegalStateException e) {
            logger.warn("Non-string cell type for column {}. Value: {}", columnName, cell);
            return null; // Return null if not a string
        }
    }

    private double getNumericCellValue(Cell cell, String columnName) {
        if (cell == null) {
            return 0.0; // або кинути виняток, якщо не може бути 0
        }
        try {
            return cell.getNumericCellValue();
        } catch (IllegalStateException e) {
            logger.warn("Non-numeric cell type for column {}. Value: {}", columnName, cell);
            return 0.0;
        }
    }

    private BigDecimal getBigDecimalCellValue(Cell cell, String columnName) {
        if (cell == null) {
            return BigDecimal.ZERO; // або null, залежить від логіки
        }
        try {
            return new BigDecimal(String.valueOf(cell.getNumericCellValue()));
        } catch (IllegalStateException | NumberFormatException e) {
            // Спроба обробки, якщо число було збережено як текст
            try {
                String strValue = cell.getStringCellValue().trim();
                if (strValue.isEmpty()) return BigDecimal.ZERO;
                return new BigDecimal(strValue);
            } catch (Exception ex) {
                logger.warn("Could not parse cell value as BigDecimal for column {}. Value: {}", columnName, cell);
                return BigDecimal.ZERO;
            }
        }
    }

    private LocalDate getLocalDateCellValue(Cell cell, String columnName) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return DateUtil.getJavaDate(cell.getNumericCellValue()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return LocalDate.parse(cell.getStringCellValue().trim(), dateFormatter);
            } catch (DateTimeParseException e) {
                logger.warn("Could not parse date string '{}' for column {}. Error: {}", cell.getStringCellValue(), columnName, e.getMessage());
                return null;
            }
        }
        logger.warn("Unsupported cell type for date column {}. Value: {}", columnName, cell);
        return null;
    }
}