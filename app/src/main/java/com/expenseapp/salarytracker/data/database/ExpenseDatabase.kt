package com.expenseapp.salarytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expenseapp.salarytracker.data.converter.DatabaseConverters
import com.expenseapp.salarytracker.data.dao.AccountDao
import com.expenseapp.salarytracker.data.dao.BudgetDao
import com.expenseapp.salarytracker.data.dao.CategoryDao
import com.expenseapp.salarytracker.data.dao.CustomFieldDao
import com.expenseapp.salarytracker.data.dao.GoalDao
import com.expenseapp.salarytracker.data.dao.RecurringRuleDao
import com.expenseapp.salarytracker.data.dao.TransactionDao
import com.expenseapp.salarytracker.data.dao.TreeHistoryDao
import com.expenseapp.salarytracker.data.dao.UserSettingDao
import com.expenseapp.salarytracker.data.entity.AccountEntity
import com.expenseapp.salarytracker.data.entity.BudgetEntity
import com.expenseapp.salarytracker.data.entity.CategoryEntity
import com.expenseapp.salarytracker.data.entity.CustomFieldEntity
import com.expenseapp.salarytracker.data.entity.GoalEntity
import com.expenseapp.salarytracker.data.entity.RecurringRuleEntity
import com.expenseapp.salarytracker.data.entity.TransactionEntity
import com.expenseapp.salarytracker.data.entity.TreeHistoryEntity
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Category
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        RecurringRuleEntity::class,
        CustomFieldEntity::class,
        UserSettingEntity::class,
        TreeHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun customFieldDao(): CustomFieldDao
    abstract fun userSettingDao(): UserSettingDao
    abstract fun treeHistoryDao(): TreeHistoryDao

    suspend fun prepopulateDefaults() {
        val catDao = categoryDao()
        if (catDao.getCount() == 0) {
            val defaultCategoryEntities = Category.DEFAULT_CATEGORIES.map {
                CategoryEntity(
                    name = it.name,
                    iconName = it.iconName,
                    colorHex = it.colorHex,
                    sortOrder = it.sortOrder
                )
            }
            catDao.insertAll(defaultCategoryEntities)
        } else {
            val existingNames = catDao.getAllCategoryNames().toSet()
            val missing = Category.DEFAULT_CATEGORIES.filter { it.name !in existingNames }
            if (missing.isNotEmpty()) {
                val missingEntities = missing.map {
                    CategoryEntity(
                        name = it.name,
                        iconName = it.iconName,
                        colorHex = it.colorHex,
                        sortOrder = it.sortOrder
                    )
                }
                catDao.insertAll(missingEntities)
            }
        }

        val accDao = accountDao()
        if (accDao.getCount() == 0) {
            val defaultAccounts = listOf(
                AccountEntity(name = "Cash", type = AccountType.CASH, colorHex = "#10B981", sortOrder = 1),
                AccountEntity(name = "Prime Bank", type = AccountType.BANK, colorHex = "#3B82F6", sortOrder = 2),
                AccountEntity(name = "bKash", type = AccountType.BKASH, colorHex = "#E11D48", sortOrder = 3),
                AccountEntity(name = "Nagad", type = AccountType.NAGAD, colorHex = "#F97316", sortOrder = 4)
            )
            accDao.insertAll(defaultAccounts)
        }

        val settingDao = userSettingDao()
        val defaultSettings = listOf(
            UserSettingEntity("salary_amount", "12000000"), // ৳120,000.00
            UserSettingEntity("currency", "BDT"),
            UserSettingEntity("currency_symbol", "৳"),
            UserSettingEntity("salary_day", "1"),
            UserSettingEntity("salary_frequency", "MONTHLY"),
            UserSettingEntity("reporting_mode", "SALARY_CYCLE")
        )
        for (setting in defaultSettings) {
            if (settingDao.getSettingDirect(setting.key) == null) {
                settingDao.setSetting(setting)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Transactions: add audit timestamp
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAtEpochMs INTEGER NOT NULL DEFAULT 0")

                // Recurring rules: add intervalDays and preferredDayOfMonth
                db.execSQL("ALTER TABLE recurring_rules ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 30")
                db.execSQL("ALTER TABLE recurring_rules ADD COLUMN preferredDayOfMonth INTEGER")

                // Goals: add accountId, status, reachedMilestonesJson, and timestamps
                db.execSQL("ALTER TABLE goals ADD COLUMN accountId INTEGER")
                db.execSQL("ALTER TABLE goals ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE goals ADD COLUMN reachedMilestonesJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE goals ADD COLUMN createdAtEpochMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN updatedAtEpochMs INTEGER NOT NULL DEFAULT 0")

                // Tree history table for meaningful state transitions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tree_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `previousState` TEXT NOT NULL,
                        `newState` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `contributingGoalId` INTEGER,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context, scope: CoroutineScope): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
