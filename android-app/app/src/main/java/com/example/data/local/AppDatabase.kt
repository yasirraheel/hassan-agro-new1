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
    version = 2,
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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hassan_agro_ledger.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
