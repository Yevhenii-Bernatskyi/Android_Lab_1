package com.example.android_lab_1.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Сутність для збереження одного запису прогнозу погоди
@Entity(tableName = "forecast_items")
data class ForecastItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "date_time_text") // Явно вказуємо назву колонки
    val dateTimeText: String, // "2025-05-19 18:00:00" - хороший унікальний ключ для прогнозу

    @ColumnInfo(name = "dt_timestamp")
    val dateTimeStamp: Long,

    val temp: Double,

    @ColumnInfo(name = "feels_like")
    val feelsLike: Double,

    val humidity: Int,
    val pressure: Int,

    @ColumnInfo(name = "weather_main")
    val weatherMain: String, // "Rain", "Clouds"

    @ColumnInfo(name = "weather_description")
    val weatherDescription: String, // "легкий дощ"

    @ColumnInfo(name = "weather_icon")
    val weatherIcon: String, // "10n"

    @ColumnInfo(name = "wind_speed")
    val windSpeed: Double,

    @ColumnInfo(name = "cloudiness_percent")
    val cloudinessPercent: Int,

    @ColumnInfo(name = "rain_volume_3h")
    val rainVolume3h: Double?, // Може бути null, якщо дощу немає

    @ColumnInfo(name = "city_name") // Для можливості фільтрації/збереження для різних міст
    val cityName: String,

    @ColumnInfo(name = "country_code")
    val countryCode: String
)

// Сутність для збереження часу останнього успішного запиту до API для конкретного міста
@Entity(tableName = "last_fetch_timestamps", primaryKeys = ["city_name_key"])
data class LastFetchTimestampEntity(
    @ColumnInfo(name = "city_name_key") // Робимо назву міста ключем
    val cityNameKey: String,
    val timestamp: Long // Час останнього запиту в мілісекундах
)