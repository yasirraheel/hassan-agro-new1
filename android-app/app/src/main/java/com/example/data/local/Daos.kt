package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE isDeleted = 0 ORDER BY createdAt ASC")
    fun observeBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BusinessEntity?

    @Query("SELECT COUNT(*) FROM businesses WHERE isDeleted = 0")
    suspend fun countActiveBusinesses(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(business: BusinessEntity)

    @Query("UPDATE businesses SET isDeleted = 1, updatedAt = :timestamp, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM businesses WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<BusinessEntity>

    @Query("UPDATE businesses SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM businesses WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE businessId = :businessId AND isDeleted = 0 ORDER BY name ASC")
    fun observeByBusiness(businessId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAllActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND barcode = :barcode AND isDeleted = 0 LIMIT 1")
    suspend fun findByBarcode(businessId: String, barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Query("UPDATE products SET stockQty = stockQty + :delta, updatedAt = :timestamp, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun adjustStock(id: String, delta: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :timestamp, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM products WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ProductEntity>

    @Query("UPDATE products SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM products WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices WHERE businessId = :businessId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeByBusiness(businessId: String): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAllActive(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getWithItems(id: String): InvoiceWithItems?

    @Query("SELECT COUNT(*) FROM invoices WHERE businessId = :businessId")
    suspend fun countForBusiness(businessId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)

    @Transaction
    suspend fun upsertInvoiceWithItems(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        upsertInvoice(invoice)
        deleteItemsForInvoice(invoice.id)
        if (items.isNotEmpty()) {
            upsertItems(items)
        }
    }

    @Transaction
    @Query("SELECT * FROM invoices WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<InvoiceWithItems>

    @Query("UPDATE invoices SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM invoices WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND isDeleted = 0 ORDER BY expenseDateMillis DESC")
    fun observeByBusiness(businessId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY expenseDateMillis DESC")
    fun observeAllActive(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Query("UPDATE expenses SET isDeleted = 1, updatedAt = :timestamp, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM expenses WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ExpenseEntity>

    @Query("UPDATE expenses SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM expenses WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>
}
