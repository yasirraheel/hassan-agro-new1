package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BusinessEntity::class,
        ProductEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        ExpenseEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val DEFAULT_BUSINESS_ID = "hassan-agro-primary"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Cash'")
                db.execSQL("ALTER TABLE invoices ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'PAID'")
                db.execSQL("ALTER TABLE invoices ADD COLUMN amountPaid REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN balanceDue REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN isReturned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE invoices SET amountPaid = grandTotal WHERE amountPaid = 0.0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE businesses ADD COLUMN themeKey TEXT NOT NULL DEFAULT 'OCEAN'")
                db.execSQL("ALTER TABLE businesses ADD COLUMN businessType TEXT NOT NULL DEFAULT 'GENERAL'")
                db.execSQL("ALTER TABLE businesses ADD COLUMN currencySymbol TEXT NOT NULL DEFAULT 'Rs.'")
                db.execSQL("ALTER TABLE businesses ADD COLUMN trackExpiry INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE businesses ADD COLUMN trackBatch INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE businesses ADD COLUMN batchFieldLabel TEXT NOT NULL DEFAULT 'Batch / Lot'")
                db.execSQL("ALTER TABLE businesses ADD COLUMN packSizeLabel TEXT NOT NULL DEFAULT 'Unit / Size'")
                db.execSQL("ALTER TABLE businesses ADD COLUMN productCategoriesJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE businesses ADD COLUMN expenseCategoriesJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE businesses ADD COLUMN paymentMethodsJson TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE businesses ADD COLUMN pricingTierLabelsJson TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE products ADD COLUMN performedById TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN performedByName TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE invoices ADD COLUMN performedById TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoices ADD COLUMN performedByName TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE expenses ADD COLUMN performedById TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE expenses ADD COLUMN performedByName TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hassan_agro_ledger.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
