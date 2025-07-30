package com.yourcompany.coffeeshop.app;

import com.yourcompany.coffeeshop.data.ExcelDataReader;
import com.yourcompany.coffeeshop.db.PostgreSQLManager;
import com.yourcompany.coffeeshop.model.InventoryItem;
import com.yourcompany.coffeeshop.model.MenuItem;
import com.yourcompany.coffeeshop.model.SaleRecord;
import com.yourcompany.coffeeshop.processor.DataProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String DATA_DIR = "data/";

    public static void main(String[] args) {
        logger.info("Starting data processing for Coffee Shop Analytics.");

        ExcelDataReader dataReader = new ExcelDataReader();
        PostgreSQLManager dbManager = new PostgreSQLManager();

        try {

            // 1. Читання даних з Excel

            List<SaleRecord> rawSales = dataReader.readDailySales(DATA_DIR + "daily_sales.xlsx");
            List<MenuItem> menuItems = dataReader.readMenuItems(DATA_DIR + "menu_items.xlsx");
            List<InventoryItem> inventoryItems = dataReader.readInventory(DATA_DIR + "inventory.xlsx");

            logger.info("Raw Sales Records read: {}", rawSales.size());
            logger.info("Menu Items read: {}", menuItems.size());
            logger.info("Inventory Items read: {}", inventoryItems.size());

            // 2. Обробка та трансформація даних
            DataProcessor dataProcessor = new DataProcessor(menuItems);
            List<SaleRecord> processedSales = dataProcessor.processSalesData(rawSales);

            logger.info("Processed Sales Records after cleaning and transformation: {}", processedSales.size());

            // Отримання унікальних імен барист для вставки

            Set<String> uniqueBaristaNames = processedSales.stream()
                    .map(SaleRecord::getBaristaName)
                    .collect(Collectors.toSet());

            // 3. Завантаження даних до PostgreSQL
            dbManager.createTables(); // Створити таблиці, якщо їх немає
            dbManager.insertBaristas(List.copyOf(uniqueBaristaNames)); // Вставляємо баристів
            dbManager.insertMenuItems(menuItems);
            dbManager.insertInventoryItems(inventoryItems);
            dbManager.insertSales(processedSales);
            logger.info("Data successfully loaded into PostgreSQL.");

            // 4. Приклад виконання аналітичного запиту з Java

            logger.info("Top 5 best-selling items:");
            dbManager.getTopSellingItems(5).forEach((itemName, quantity) ->
                    logger.info("- {}: {} units", itemName, quantity)
            );

        } catch (SQLException e) { // Цей блок залишаємо, бо методи dbManager можуть кидати SQLException
            logger.error("Database error occurred: {}", e.getMessage(), e);
        } catch (Exception e) { // Залишаємо загальний виняток для інших непередбачених помилок
            logger.error("An unexpected error occurred: {}", e.getMessage(), e);
        }

        logger.info("Data processing finished.");
    }
}