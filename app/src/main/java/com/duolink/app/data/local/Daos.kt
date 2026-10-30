package com.duolink.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PairingDao {
    @Query("SELECT * FROM pairing_config WHERE id = 1 LIMIT 1")
    fun getPairing(): Flow<PairingEntity?>

    @Query("SELECT * FROM pairing_config WHERE id = 1 LIMIT 1")
    suspend fun getPairingOnce(): PairingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePairing(pairing: PairingEntity)

    @Query("UPDATE pairing_config SET peerDisplayName = :newNickname WHERE id = 1")
    suspend fun updatePeerNickname(newNickname: String): Int

    @Query("DELETE FROM pairing_config")
    suspend fun clearPairing(): Int
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMessage(): Flow<MessageEntity?>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageOnce(): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE senderDeviceId != :myDeviceId AND status != 'READ'")
    fun getUnreadCount(myDeviceId: String): Flow<Int>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMessages(query: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: MessageStatus): Int

    @Query("UPDATE messages SET status = 'READ' WHERE senderDeviceId != :myDeviceId AND status != 'READ'")
    suspend fun markAllIncomingAsRead(myDeviceId: String): Int

    @Query("UPDATE messages SET content = :newEncryptedContent, isEdited = 1 WHERE id = :id")
    suspend fun editMessage(id: String, newEncryptedContent: String): Int

    // Strictly Permanent Deletion for Everyone
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deletePermanently(id: String): Int

    @Query("DELETE FROM messages WHERE timestamp <= :timestamp")
    suspend fun clearMessagesBefore(timestamp: Long): Int

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages(): Int
}

@Dao
interface TombstoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTombstone(tombstone: DeletionTombstoneEntity)

    @Query("SELECT COUNT(*) FROM deletion_tombstones WHERE messageId = :messageId")
    suspend fun isMessageTombstoned(messageId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistoryTombstone(tombstone: HistoryTombstoneEntity)

    @Query("SELECT * FROM history_tombstones WHERE id = 1 LIMIT 1")
    suspend fun getHistoryTombstone(): HistoryTombstoneEntity?

    @Query("DELETE FROM deletion_tombstones")
    suspend fun clearTombstones(): Int
}

@Dao
interface PendingDeletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(pending: PendingDeletionEntity)

    @Query("SELECT * FROM pending_deletions ORDER BY timestamp ASC")
    suspend fun getAllPending(): List<PendingDeletionEntity>

    @Query("DELETE FROM pending_deletions WHERE id = :id")
    suspend fun removePending(id: String): Int

    @Query("DELETE FROM pending_deletions")
    suspend fun clearAllPending(): Int
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY startedAt DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE startedAt <= :timestamp")
    suspend fun clearCallsBefore(timestamp: Long): Int

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs(): Int
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox_queue WHERE status = 'PENDING' ORDER BY queuedAt ASC")
    suspend fun getPendingOutbox(): List<OutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(outbox: OutboxEntity)

    @Query("DELETE FROM outbox_queue WHERE messageId = :messageId")
    suspend fun remove(messageId: String): Int

    @Query("UPDATE outbox_queue SET retryCount = retryCount + 1, status = :status WHERE messageId = :messageId")
    suspend fun updateRetry(messageId: String, status: String): Int

    @Query("DELETE FROM outbox_queue")
    suspend fun clearOutbox(): Int
}
