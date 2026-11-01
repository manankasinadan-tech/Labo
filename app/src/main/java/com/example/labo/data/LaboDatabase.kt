package com.example.labo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, FileRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LaboDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun fileRecordDao(): FileRecordDao

    companion object {
        @Volatile
        private var INSTANCE: LaboDatabase? = null

        fun getInstance(context: Context): LaboDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LaboDatabase::class.java,
                    "labo_workbench.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
