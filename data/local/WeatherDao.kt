package com.example.android_lab_1.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow // Для отримання даних як потоку, що автоматично оновлюється

@Dao
interface WeatherDao {

    // --- Методи для ForecastItemEntity ---

    // Вставити список елементів прогнозу.
    // OnConflictStrategy.REPLACE означає, що якщо елемент з таким же Primary Key вже існує, він буде замінений.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecastItems(items: List<ForecastItemEntity>)

    // Отримати всі елементи прогнозу для конкретного міста, відсортовані за часом.
    // Повертає Flow, що дозволяє UI автоматично оновлюватися при зміні даних в БД.
    @Query("SELECT * FROM forecast_items WHERE city_name = :cityName ORDER BY dt_timestamp ASC")
    fun getForecastByCity(cityName: String): Flow<List<ForecastItemEntity>>

    // Отримати всі елементи прогнозу для конкретного міста ОДИН РАЗ (не Flow).
    // Це може бути корисно для перевірки, чи є дані в кеші, без підписки на Flow.
    @Query("SELECT * FROM forecast_items WHERE city_name = :cityName ORDER BY dt_timestamp ASC")
    suspend fun getForecastByCityOnce(cityName: String): List<ForecastItemEntity>

    // Видалити всі записи прогнозу для конкретного міста.
    // Це потрібно перед вставкою нових даних, щоб уникнути дублікатів або застарілих даних.
    @Query("DELETE FROM forecast_items WHERE city_name = :cityName")
    suspend fun deleteForecastByCity(cityName: String)

    // --- Методи для LastFetchTimestampEntity ---

    // Вставити або оновити час останнього запиту для міста.
    // Оскільки cityNameKey є Primary Key, OnConflictStrategy.REPLACE оновить запис, якщо він існує.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateLastFetchTimestamp(timestampEntity: LastFetchTimestampEntity)

    // Отримати час останнього запиту для конкретного міста.
    @Query("SELECT * FROM last_fetch_timestamps WHERE city_name_key = :cityName")
    suspend fun getLastFetchTimestamp(cityName: String): LastFetchTimestampEntity? // Може бути null, якщо записів ще немає
}