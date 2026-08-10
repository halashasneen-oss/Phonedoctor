package com.screenlens.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query(
        """SELECT * FROM history
           WHERE (:type IS NULL OR type = :type)
           AND (:query = '' OR originalText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%')
           ORDER BY createdAt DESC"""
    )
    fun observeFiltered(type: String?, query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getById(id: Long): HistoryEntity?

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int

    @Query(
        """DELETE FROM history WHERE id IN (
            SELECT id FROM history ORDER BY createdAt DESC LIMIT -1 OFFSET :keepMostRecent
        )"""
    )
    suspend fun trimToLimit(keepMostRecent: Int)
}
