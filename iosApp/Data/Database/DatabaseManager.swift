import Foundation
import SQLite3

/**
 * 100% Offline SQLite Database Manager for iOS.
 * Implements identical schema and invariants as the Android Room Database.
 */
class DatabaseManager {
    static let shared = DatabaseManager()
    
    private var db: OpaquePointer?
    private let dbQueue = DispatchQueue(label: "com.expenseapp.salarytracker.db", qos: .userInitiated)
    
    private init() {
        openDatabase()
        createTables()
        prepopulateDefaultsIfNeeded()
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
    
    private func openDatabase() {
        do {
            let fileURL = try FileManager.default
                .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
                .appendingPathComponent("expense_tracker.sqlite")
            
            if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
                print("Error opening SQLite database at \(fileURL.path)")
            }
        } catch {
            print("Failed to resolve documents directory for SQLite: \(error)")
        }
    }
    
    private func createTables() {
        dbQueue.sync {
            let schemaSql = """
            PRAGMA foreign_keys = ON;
            
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                initialBalanceMinorUnits INTEGER NOT NULL DEFAULT 0,
                currency TEXT NOT NULL DEFAULT 'BDT',
                isArchived INTEGER NOT NULL DEFAULT 0,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                colorHex TEXT NOT NULL DEFAULT '#10B981'
            );
            
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                iconName TEXT NOT NULL DEFAULT 'category',
                colorHex TEXT NOT NULL DEFAULT '#3B82F6',
                parentCategoryId INTEGER,
                isArchived INTEGER NOT NULL DEFAULT 0,
                sortOrder INTEGER NOT NULL DEFAULT 0
            );
            
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amountMinorUnits INTEGER NOT NULL,
                type TEXT NOT NULL,
                accountId INTEGER NOT NULL,
                destinationAccountId INTEGER,
                categoryId INTEGER,
                subcategoryId INTEGER,
                date TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '[]',
                recurringRuleId INTEGER,
                originalTransactionId INTEGER,
                customFieldValues TEXT NOT NULL DEFAULT '{}',
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                FOREIGN KEY (accountId) REFERENCES accounts(id) ON DELETE CASCADE
            );
            
            CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date);
            CREATE INDEX IF NOT EXISTS index_transactions_account ON transactions(accountId);
            
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER,
                limitAmountMinorUnits INTEGER NOT NULL,
                period TEXT NOT NULL DEFAULT 'SALARY_CYCLE',
                startDate TEXT,
                endDate TEXT,
                warningThresholdPercentage INTEGER NOT NULL DEFAULT 80
            );
            
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                type TEXT NOT NULL,
                targetAmountMinorUnits INTEGER NOT NULL DEFAULT 0,
                targetDays INTEGER NOT NULL DEFAULT 0,
                categoryId INTEGER,
                accountId INTEGER,
                startDate TEXT NOT NULL,
                endDate TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                reachedMilestonesJson TEXT NOT NULL DEFAULT '[]',
                treeState TEXT NOT NULL DEFAULT 'SEED',
                createdAtEpochMs INTEGER NOT NULL DEFAULT 0,
                updatedAtEpochMs INTEGER NOT NULL DEFAULT 0
            );
            
            CREATE TABLE IF NOT EXISTS user_settings (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL
            );
            """
            
            if sqlite3_exec(db, schemaSql, nil, nil, nil) != SQLITE_OK {
                let errMsg = String(cString: sqlite3_errmsg(db))
                print("Failed to initialize schema: \(errMsg)")
            }
        }
    }
    
    private func prepopulateDefaultsIfNeeded() {
        dbQueue.sync {
            // Prepopulate Categories if empty
            var countStatement: OpaquePointer?
            if sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM categories", -1, &countStatement, nil) == SQLITE_OK {
                if sqlite3_step(countStatement) == SQLITE_ROW {
                    let count = sqlite3_column_int(countStatement, 0)
                    if count == 0 {
                        let categories = [
                            ("Transport", "directions_bus", "#3B82F6", 1),
                            ("Food", "restaurant", "#F59E0B", 2),
                            ("Groceries", "shopping_cart", "#10B981", 3),
                            ("Rent", "home", "#8B5CF6", 4),
                            ("Utilities", "bolt", "#EF4444", 5),
                            ("Mobile", "smartphone", "#06B6D4", 6),
                            ("Internet", "wifi", "#6366F1", 7),
                            ("Shopping", "shopping_bag", "#EC4899", 8),
                            ("Entertainment", "movie", "#F97316", 9),
                            ("Health", "medical_services", "#14B8A6", 10),
                            ("Education", "school", "#84CC16", 11),
                            ("Donation", "volunteer_activism", "#E11D48", 12),
                            ("Savings", "savings", "#059669", 13),
                            ("Salary / Income", "payments", "#16A34A", 14),
                            ("Other", "more_horiz", "#94A3B8", 15)
                        ]
                        
                        var insertStmt: OpaquePointer?
                        let insertSql = "INSERT INTO categories (name, iconName, colorHex, sortOrder) VALUES (?, ?, ?, ?);"
                        if sqlite3_prepare_v2(db, insertSql, -1, &insertStmt, nil) == SQLITE_OK {
                            for cat in categories {
                                sqlite3_bind_text(insertStmt, 1, (cat.0 as NSString).utf8String, -1, nil)
                                sqlite3_bind_text(insertStmt, 2, (cat.1 as NSString).utf8String, -1, nil)
                                sqlite3_bind_text(insertStmt, 3, (cat.2 as NSString).utf8String, -1, nil)
                                sqlite3_bind_int(insertStmt, 4, Int32(cat.3))
                                sqlite3_step(insertStmt)
                                sqlite3_reset(insertStmt)
                            }
                        }
                        sqlite3_finalize(insertStmt)
                    }
                }
            }
            sqlite3_finalize(countStatement)
        }
    }
    
    // Core Transaction Operations
    func insertTransaction(amount: Int64, type: String, accountId: Int64, categoryId: Int64?, date: String, note: String) -> Int64 {
        let insertedId = dbQueue.sync { () -> Int64 in
            var statement: OpaquePointer?
            let sql = """
            INSERT INTO transactions (amountMinorUnits, type, accountId, categoryId, date, note, createdAtEpochMs, updatedAtEpochMs)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """
            var id: Int64 = -1
            if sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK {
                let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
                sqlite3_bind_int64(statement, 1, amount)
                sqlite3_bind_text(statement, 2, (type as NSString).utf8String, -1, nil)
                sqlite3_bind_int64(statement, 3, accountId)
                if let catId = categoryId {
                    sqlite3_bind_int64(statement, 4, catId)
                } else {
                    sqlite3_bind_null(statement, 4)
                }
                sqlite3_bind_text(statement, 5, (date as NSString).utf8String, -1, nil)
                sqlite3_bind_text(statement, 6, (note as NSString).utf8String, -1, nil)
                sqlite3_bind_int64(statement, 7, nowMs)
                sqlite3_bind_int64(statement, 8, nowMs)
                
                if sqlite3_step(statement) == SQLITE_DONE {
                    id = sqlite3_last_insert_rowid(db)
                }
            }
            sqlite3_finalize(statement)
            return id
        }
        
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: NSNotification.Name("DatabaseDataChanged"), object: nil)
        }
        return insertedId
    }
    
    // Read Transactions
    func fetchAllTransactions() -> [TransactionRecord] {
        return dbQueue.sync {
            var records: [TransactionRecord] = []
            let sql = """
            SELECT t.id, t.amountMinorUnits, t.type, a.name, COALESCE(c.name, 'Uncategorized'), t.date, t.note
            FROM transactions t
            LEFT JOIN accounts a ON t.accountId = a.id
            LEFT JOIN categories c ON t.categoryId = c.id
            ORDER BY t.createdAtEpochMs DESC;
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let id = sqlite3_column_int64(stmt, 0)
                    let amount = sqlite3_column_int64(stmt, 1)
                    let type = String(cString: sqlite3_column_text(stmt, 2))
                    let account = String(cString: sqlite3_column_text(stmt, 3))
                    let category = String(cString: sqlite3_column_text(stmt, 4))
                    let date = String(cString: sqlite3_column_text(stmt, 5))
                    let note = String(cString: sqlite3_column_text(stmt, 6))
                    records.append(TransactionRecord(id: id, amount: amount, type: type, account: account, category: category, date: date, note: note))
                }
            }
            sqlite3_finalize(stmt)
            return records
        }
    }
    
    func fetchNetBalanceMinor() -> Int64 {
        return dbQueue.sync {
            var net: Int64 = 0
            let sql = """
            SELECT 
                COALESCE(SUM(CASE WHEN type IN ('INCOME', 'Income', 'Debt Inflow') THEN amountMinorUnits 
                                  WHEN type IN ('EXPENSE', 'Expense', 'Debt Repayment') THEN -amountMinorUnits 
                                  ELSE 0 END), 0)
            FROM transactions;
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                if sqlite3_step(stmt) == SQLITE_ROW {
                    net = sqlite3_column_int64(stmt, 0)
                }
            }
            sqlite3_finalize(stmt)
            return net
        }
    }
    
    func fetchTotalSpentMinor() -> Int64 {
        return dbQueue.sync {
            var spent: Int64 = 0
            let sql = "SELECT COALESCE(SUM(amountMinorUnits), 0) FROM transactions WHERE type IN ('EXPENSE', 'Expense');"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                if sqlite3_step(stmt) == SQLITE_ROW {
                    spent = sqlite3_column_int64(stmt, 0)
                }
            }
            sqlite3_finalize(stmt)
            return spent
        }
    }
    
    func fetchCategorySpendingBreakdown() -> [CategoryBreakdownRecord] {
        return dbQueue.sync {
            var records: [CategoryBreakdownRecord] = []
            let sql = """
            SELECT COALESCE(c.name, 'Other'), c.colorHex, SUM(t.amountMinorUnits) as totalSpent
            FROM transactions t
            LEFT JOIN categories c ON t.categoryId = c.id
            WHERE t.type IN ('EXPENSE', 'Expense')
            GROUP BY c.name, c.colorHex
            ORDER BY totalSpent DESC;
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let name = String(cString: sqlite3_column_text(stmt, 0))
                    let hex = sqlite3_column_text(stmt, 1) != nil ? String(cString: sqlite3_column_text(stmt, 1)) : "#10B981"
                    let amount = sqlite3_column_int64(stmt, 2)
                    records.append(CategoryBreakdownRecord(name: name, colorHex: hex, amountMinor: amount))
                }
            }
            sqlite3_finalize(stmt)
            return records
        }
    }
    
    func fetchWalletsBalances() -> [WalletBalanceRecord] {
        return dbQueue.sync {
            var records: [WalletBalanceRecord] = []
            let sql = """
            SELECT a.name, a.colorHex, 
                   COALESCE(SUM(CASE WHEN t.type IN ('INCOME', 'Income', 'Debt Inflow') THEN t.amountMinorUnits 
                                     WHEN t.type IN ('EXPENSE', 'Expense', 'Debt Repayment') THEN -t.amountMinorUnits 
                                     ELSE 0 END), a.initialBalanceMinorUnits) as currentBalance
            FROM accounts a
            LEFT JOIN transactions t ON a.id = t.accountId
            GROUP BY a.id, a.name, a.colorHex
            ORDER BY a.sortOrder ASC;
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let name = String(cString: sqlite3_column_text(stmt, 0))
                    let hex = sqlite3_column_text(stmt, 1) != nil ? String(cString: sqlite3_column_text(stmt, 1)) : "#3B82F6"
                    let bal = sqlite3_column_int64(stmt, 2)
                    records.append(WalletBalanceRecord(name: name, colorHex: hex, balanceMinor: bal))
                }
            }
            sqlite3_finalize(stmt)
            return records
        }
    }
    
    // Full Reset: Purge all data & re-seed defaults
    func resetAllData() {
        dbQueue.sync {
            let wipeSql = """
            DELETE FROM transactions;
            DELETE FROM budgets;
            DELETE FROM goals;
            DELETE FROM user_settings;
            DELETE FROM accounts;
            DELETE FROM categories;
            """
            sqlite3_exec(db, wipeSql, nil, nil, nil)
        }
        prepopulateDefaultsIfNeeded()
        DispatchQueue.main.async {
            NotificationCenter.default.post(name: NSNotification.Name("DatabaseDataChanged"), object: nil)
        }
    }
}

struct TransactionRecord: Identifiable {
    let id: Int64
    let amount: Int64
    let type: String
    let account: String
    let category: String
    let date: String
    let note: String
}

struct CategoryBreakdownRecord: Identifiable {
    var id: String { name }
    let name: String
    let colorHex: String
    let amountMinor: Int64
}

struct WalletBalanceRecord: Identifiable {
    var id: String { name }
    let name: String
    let colorHex: String
    let balanceMinor: Int64
}
