package com.example.offlinepos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptTemplateDao {
    @Query("SELECT * FROM receipt_templates ORDER BY templateId ASC")
    fun getAllTemplates(): Flow<List<ReceiptTemplateEntity>>

    @Query("SELECT * FROM receipt_templates WHERE templateId = :id LIMIT 1")
    suspend fun getTemplateById(id: Int): ReceiptTemplateEntity?

    @Query("SELECT * FROM receipt_templates WHERE isDefault = 1 LIMIT 1")
    fun getDefaultTemplate(): Flow<ReceiptTemplateEntity?>

    @Query("SELECT * FROM receipt_templates WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTemplateSync(): ReceiptTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(template: ReceiptTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ReceiptTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: ReceiptTemplateEntity)

    @Query("SELECT COUNT(*) FROM receipt_templates")
    suspend fun getTemplateCount(): Int

    @Transaction
    suspend fun setDefaultTemplate(targetId: Int) {
        clearDefaultFlag()
        setSingleDefault(targetId)
    }

    @Query("UPDATE receipt_templates SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE receipt_templates SET isDefault = 1 WHERE templateId = :targetId")
    suspend fun setSingleDefault(targetId: Int)
}
