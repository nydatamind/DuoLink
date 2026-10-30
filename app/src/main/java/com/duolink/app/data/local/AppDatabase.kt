package com.duolink.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PairingEntity::class,
        MessageEntity::class,
        CallLogEntity::class,
        OutboxEntity::class,
        DeletionTombstoneEntity::class,
        HistoryTombstoneEntity::class,
        PendingDeletionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pairingDao(): PairingDao
    abstract fun messageDao(): MessageDao
    abstract fun callLogDao(): CallLogDao
    abstract fun outboxDao(): OutboxDao
    abstract fun tombstoneDao(): TombstoneDao
    abstract fun pendingDeletionDao(): PendingDeletionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "duolink_secure.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
