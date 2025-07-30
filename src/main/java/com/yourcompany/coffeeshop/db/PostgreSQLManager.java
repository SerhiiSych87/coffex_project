package com.yourcompany.coffeeshop.db;

import com.yourcompany.coffeeshop.model.InventoryItem;
import com.yourcompany.coffeeshop.model.MenuItem;
import com.yourcompany.coffeeshop.model.SaleRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

public class PostgreSQLManager {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLManager.class);

    // Параметри підключення до бази даних (краще зберігати у файлі конфігурації!)

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/coffee_shop_db"; // шлях до нашої БД в pgAdmain
    private static final String USER = "serhii_project"; // ім'я користувача PostgreSQL
    private static final String PASSWORD = "77778888"; // пароль PostgreSQL

    public PostgreSQLManager() {
        // Конструктор

        logger.info("PostgreSQLManager initialized.");
    }

    // --- Метод для встановлення з'єднання з БД ---
    private Connection getConnection() throws SQLException {
        logger.debug("Attempting to connect to database: {}", DB_URL);
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    // --- Метод для створення таблиць SQL ---

    public void createTables() throws SQLException {
        String createBaristasTableSQL = "CREATE TABLE IF NOT EXISTS baristas ("
                + "barista_id SERIAL PRIMARY KEY,"
                + "name VARCHAR(255) UNIQUE NOT NULL"
                + ");";

        String createMenuItemsTableSQL = "CREATE TABLE IF NOT EXISTS menu_items ("
                + "item_id SERIAL PRIMARY KEY,"
                + "name VARCHAR(255) UNIQUE NOT NULL,"
                + "category VARCHAR(100),"
                + "standard_price NUMERIC(10, 2) NOT NULL,"
                + "cost_per_unit NUMERIC(10, 2)"
                + ");";

        String createIngredientsTableSQL = "CREATE TABLE IF NOT EXISTS ingredients ("
                + "ingredient_id SERIAL PRIMARY KEY,"
                + "name VARCHAR(255) UNIQUE NOT NULL,"
                + "unit_of_measure VARCHAR(50),"
                + "unit_cost NUMERIC(10, 2)"
                + ");";

        String createInventoryLogsTableSQL = "CREATE TABLE IF NOT EXISTS inventory_logs ("
                + "log_id SERIAL PRIMARY KEY,"
                + "ingredient_id INT REFERENCES ingredients(ingredient_id),"
                + "log_date DATE NOT NULL,"
                + "change_amount NUMERIC(10, 2) NOT NULL,"
                + "reason VARCHAR(255)"
                + ");";

        String createSalesTableSQL = "CREATE TABLE IF NOT EXISTS sales ("
                + "sale_id INT PRIMARY KEY," // Зверніть увагу, що sale_id вже є в даних, тому не SERIAL
                + "sale_date DATE NOT NULL,"
                + "item_id INT REFERENCES menu_items(item_id),"
                + "quantity_sold INT NOT NULL,"
                + "total_price NUMERIC(10, 2) NOT NULL,"
                + "profit NUMERIC(10, 2)," // Додамо поле для розрахованого прибутку
                + "payment_method VARCHAR(50),"
                + "barista_id INT REFERENCES baristas(barista_id)"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            logger.info("Creating baristas table...");
            stmt.execute(createBaristasTableSQL);
            logger.info("Creating menu_items table...");
            stmt.execute(createMenuItemsTableSQL);
            logger.info("Creating ingredients table...");
            stmt.execute(createIngredientsTableSQL);
            logger.info("Creating inventory_logs table...");
            stmt.execute(createInventoryLogsTableSQL);
            logger.info("Creating sales table...");
            stmt.execute(createSalesTableSQL);

            logger.info("All tables created or already exist.");

        } catch (SQLException e) {
            logger.error("Error creating tables: {}", e.getMessage(), e);
            throw e; // Прокидаємо виняток, щоб Main міг його обробити
        }
    }

    // --- Метод для вставки баристів (унікальних) ---

    public void insertBaristas(List<String> baristaNames) throws SQLException {
        String sql = "INSERT INTO baristas (name) VALUES (?) ON CONFLICT (name) DO NOTHING;"; // Ігнорувати, якщо бариста вже є
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (String name : baristaNames) {
                if (name != null && !name.trim().isEmpty()) {
                    pstmt.setString(1, name.trim());
                    pstmt.addBatch(); // Додаємо до пакетної обробки
                }
            }
            int[] insertedRows = pstmt.executeBatch(); // Виконуємо пакетну вставку
            logger.info("Inserted/updated {} baristas.", insertedRows.length);
        } catch (SQLException e) {
            logger.error("Error inserting baristas: {}", e.getMessage(), e);
            throw e;
        }
    }

    // --- Метод для вставки пунктів меню ---

    public void insertMenuItems(List<MenuItem> menuItems) throws SQLException {
        // Ми не використовуємо ON CONFLICT (item_id) DO UPDATE, бо item_id приходить ззовні.
        // Якщо треба оновлювати існуючі, то використовуйте ON CONFLICT (item_id) DO UPDATE SET ...
        String sql = "INSERT INTO menu_items (item_id, name, category, standard_price, cost_per_unit) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT (item_id) DO UPDATE SET "
                + "name = EXCLUDED.name, category = EXCLUDED.category, "
                + "standard_price = EXCLUDED.standard_price, cost_per_unit = EXCLUDED.cost_per_unit;";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (MenuItem item : menuItems) {
                pstmt.setInt(1, item.getItemId());
                pstmt.setString(2, item.getNameStandard());
                pstmt.setString(3, item.getCategory());
                pstmt.setBigDecimal(4, item.getStandardPrice());
                pstmt.setBigDecimal(5, item.getCostPerUnit());
                pstmt.addBatch();
            }
            int[] insertedRows = pstmt.executeBatch();
            logger.info("Inserted/updated {} menu items.", insertedRows.length);
        } catch (SQLException e) {
            logger.error("Error inserting menu items: {}", e.getMessage(), e);
            throw e;
        }
    }

    // --- Метод для вставки інвентарних позицій ---
    public void insertInventoryItems(List<InventoryItem> inventoryItems) throws SQLException {
        String sql = "INSERT INTO ingredients (ingredient_id, name, unit_of_measure, unit_cost) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (ingredient_id) DO UPDATE SET "
                + "name = EXCLUDED.name, unit_of_measure = EXCLUDED.unit_of_measure, "
                + "unit_cost = EXCLUDED.unit_cost;"; // Оновлюємо дані інгредієнта

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (InventoryItem item : inventoryItems) {
                pstmt.setInt(1, item.getIngredientId());
                pstmt.setString(2, item.getIngredientName());
                // Unit of measure not explicitly in InventoryItem, assuming 'kg' or 'L' based on name or separate mapping
                pstmt.setString(3, "kg/L"); // TODO: Adjust based on your actual data or add to model
                pstmt.setBigDecimal(4, item.getUnitCost());
                pstmt.addBatch();
            }
            int[] insertedRows = pstmt.executeBatch();
            logger.info("Inserted/updated {} ingredient definitions.", insertedRows.length);

            // Також вставимо початкові дані про запаси в inventory_logs
            insertInitialInventoryLogs(inventoryItems);

        } catch (SQLException e) {
            logger.error("Error inserting inventory items (ingredients): {}", e.getMessage(), e);
            throw e;
        }
    }

    // --- Додатковий метод для початкових записів запасів ---
    private void insertInitialInventoryLogs(List<InventoryItem> inventoryItems) throws SQLException {
        String sql = "INSERT INTO inventory_logs (ingredient_id, log_date, change_amount, reason) VALUES (?, ?, ?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (InventoryItem item : inventoryItems) {
                // Перевіряємо, чи є вже запис про початковий запас для цього інгредієнта на цю дату
                // Це дуже спрощено, в реальному проєкті треба перевіряти, чи цей лог вже існує,
                // або обробляти унікальність (наприклад, UNIQUE (ingredient_id, log_date, reason)
                // Але для портфоліо може бути достатньо такої логіки:
                if (item.getLastRestockDate() != null) {
                    pstmt.setInt(1, item.getIngredientId());
                    pstmt.setDate(2, Date.valueOf(item.getLastRestockDate()));
                    pstmt.setBigDecimal(3, item.getCurrentStockKgL()); // Вважаємо поточний запас як початковий лог
                    pstmt.setString(4, "Initial Stock / Last Restock");
                    pstmt.addBatch();
                }
            }
            int[] insertedLogs = pstmt.executeBatch();
            logger.info("Inserted {} initial inventory log entries.", insertedLogs.length);
        } catch (SQLException e) {
            logger.error("Error inserting initial inventory logs: {}", e.getMessage(), e);
            throw e;
        }
    }


    // --- Метод для вставки записів про продажі ---
    public void insertSales(List<SaleRecord> sales) throws SQLException {
        // Ми використовуємо sale_id як PK, і він вже приходить з даних, тому ON CONFLICT (sale_id) DO UPDATE
        // дозволить нам перезаписувати записи, якщо ви запускаєте скрипт кілька разів.
        // Це корисно для тестування.
        String sql = "INSERT INTO sales (sale_id, sale_date, item_id, quantity_sold, total_price, profit, payment_method, barista_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (sale_id) DO UPDATE SET "
                + "sale_date = EXCLUDED.sale_date, item_id = EXCLUDED.item_id, quantity_sold = EXCLUDED.quantity_sold, "
                + "total_price = EXCLUDED.total_price, profit = EXCLUDED.profit, "
                + "payment_method = EXCLUDED.payment_method, barista_id = EXCLUDED.barista_id;";

        // Для того, щоб зв'язати barista_name зі sales.barista_id, нам потрібні ID баристів.
        // Зробимо кеш імен баристів та їх ID з БД.
        Map<String, Integer> baristaNameToIdMap = getBaristaNameToIdMap();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (SaleRecord sale : sales) {
                Integer baristaId = baristaNameToIdMap.get(sale.getBaristaName());
                if (baristaId == null) {
                    // Якщо баристи немає в базі, спробуємо вставити його та оновити мапу
                    // У реальному проекті тут краще викликати окремий метод для вставки нового баристи
                    logger.warn("Barista '{}' not found in database. Inserting...", sale.getBaristaName());
                    insertBaristas(List.of(sale.getBaristaName())); // Вставляємо нового баристу
                    baristaNameToIdMap = getBaristaNameToIdMap(); // Оновлюємо мапу
                    baristaId = baristaNameToIdMap.get(sale.getBaristaName());
                }

                // Щоб item_id був коректним, вам потрібна мапа стандартизованих назв товарів на їх ID
                // Це можна зробити, передавши Map<String, Integer> з DataProcessor або зчитавши з БД
                // Для простоти, припустимо, що item_id можна отримати з MenuItem, якщо ви його зберегли
                // Або вам потрібно буде зробити lookup по `nameStandard` в таблиці `menu_items`
                // Передбачаємо, що SaleRecord має метод getItemIdStandard() або схожий, який ви могли додати
                // Або зробимо lookup тут:
                Integer menuItemId = getMenuItemIdByName(sale.getItemNameStandard());
                if (menuItemId == null) {
                    logger.error("Menu item ID not found for standardized name: {}. Skipping sale {}.", sale.getItemNameStandard(), sale.getSaleId());
                    continue; // Пропускаємо цей продаж, якщо не знайшли товар
                }


                pstmt.setInt(1, sale.getSaleId());
                pstmt.setDate(2, Date.valueOf(sale.getDate())); // Перетворення LocalDate на java.sql.Date
                pstmt.setInt(3, menuItemId); // item_id з menu_items
                pstmt.setInt(4, sale.getQuantity());
                pstmt.setBigDecimal(5, sale.getTotalSalePrice());
                pstmt.setBigDecimal(6, sale.getTotalProfit()); // Вставляємо розрахований прибуток
                pstmt.setString(7, sale.getPaymentMethod());
                pstmt.setInt(8, baristaId); // ID баристи

                pstmt.addBatch();
            }
            int[] insertedRows = pstmt.executeBatch();
            logger.info("Inserted/updated {} sales records.", insertedRows.length);
        } catch (SQLException e) {
            logger.error("Error inserting sales: {}", e.getMessage(), e);
            throw e;
        }
    }

    // --- Допоміжні методи для отримання ID з БД ---

    // Отримання мапи імен баристів до їх ID
    private Map<String, Integer> getBaristaNameToIdMap() throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT name, barista_id FROM baristas;";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("name"), rs.getInt("barista_id"));
            }
        }
        return map;
    }

    // Отримання ID пункту меню за стандартизованою назвою
    private Integer getMenuItemIdByName(String itemName) throws SQLException {
        String sql = "SELECT item_id FROM menu_items WHERE name = ?;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("item_id");
                }
            }
        }
        return null; // Повернути null, якщо не знайдено
    }


    // --- Приклад аналітичного запиту з Java ---
    public Map<String, Long> getTopSellingItems(int limit) throws SQLException {
        Map<String, Long> topItems = new HashMap<>();
        String sql = "SELECT mi.name, SUM(s.quantity_sold) AS total_sold " +
                "FROM sales s " +
                "JOIN menu_items mi ON s.item_id = mi.item_id " +
                "GROUP BY mi.name " +
                "ORDER BY total_sold DESC " +
                "LIMIT ?;";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topItems.put(rs.getString("name"), rs.getLong("total_sold"));
                }
            }
        }
        return topItems;
    }
}