package com.example.labo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModifiedDate DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface FileRecordDao {
    @Query("SELECT * FROM file_records WHERE projectId = :projectId ORDER BY relativePath ASC")
    suspend fun getRecordsByProject(projectId: String): List<FileRecordEntity>

    @Query("SELECT * FROM file_records WHERE projectId = :projectId AND relativePath = :relativePath LIMIT 1")
    suspend fun getRecord(projectId: String, relativePath: String): FileRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<FileRecordEntity>)

    @Update
    suspend fun updateRecord(record: FileRecordEntity)

    @Query("DELETE FROM file_records WHERE projectId = :projectId")
    suspend fun deleteRecordsByProject(projectId: String)
}
