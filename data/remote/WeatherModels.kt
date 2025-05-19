package com.example.android_lab_1.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Головний клас відповіді
@Serializable
data class WeatherApiResponseDto(
    val cod: String,
    val message: Int, // Або Double, якщо може бути дробовим
    val cnt: Int,
    val list: List<ForecastListItemDto>,
    val city: CityDto
)

// Елемент списку прогнозу
@Serializable
data class ForecastListItemDto(
    val dt: Long, // Час прогнозу, Unix, UTC
    val main: MainMetricsDto,
    val weather: List<WeatherDescriptionDto>,
    val clouds: CloudsDto,
    val wind: WindDto,
    val visibility: Int,
    val pop: Double, // Ймовірність опадів
    val rain: RainDto? = null, // Може бути відсутнім, якщо немає дощу
    val sys: SysDto,
    @SerialName("dt_txt") // Використовуємо SerialName, бо назва в Kotlin відрізняється від JSON
    val dateTimeText: String // Текстове представлення дати та часу
)

// Основні погодні показники
@Serializable
data class MainMetricsDto(
    val temp: Double,
    @SerialName("feels_like")
    val feelsLike: Double,
    @SerialName("temp_min")
    val tempMin: Double,
    @SerialName("temp_max")
    val tempMax: Double,
    val pressure: Int,
    @SerialName("sea_level")
    val seaLevel: Int,
    @SerialName("grnd_level")
    val groundLevel: Int,
    val humidity: Int,
    @SerialName("temp_kf")
    val tempKf: Double // Внутрішній параметр
)

// Опис погоди (може бути масив, але зазвичай один елемент)
@Serializable
data class WeatherDescriptionDto(
    val id: Int, // ID погодних умов
    val main: String, // Група погодних умов (Rain, Snow, Clouds etc.)
    val description: String, // Опис погоди
    val icon: String // ID іконки погоди
)

// Інформація про хмарність
@Serializable
data class CloudsDto(
    val all: Int // Хмарність у %
)

// Інформація про вітер
@Serializable
data class WindDto(
    val speed: Double, // Швидкість вітру
    val deg: Int, // Напрямок вітру в градусах
    val gust: Double? = null // Пориви вітру, може бути відсутнім
)

// Інформація про дощ (за останні 3 години)
@Serializable
data class RainDto(
    @SerialName("3h")
    val threeHourVolume: Double? = null // Об'єм опадів, може бути відсутнім, якщо немає об'єкту rain
)

// Системна інформація (частина дня)
@Serializable
data class SysDto(
    val pod: String // Частина дня (d = день, n = ніч)
)

// Інформація про місто
@Serializable
data class CityDto(
    val id: Int,
    val name: String,
    val coord: CoordinatesDto,
    val country: String,
    val population: Int,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

// Координати
@Serializable
data class CoordinatesDto(
    val lat: Double,
    val lon: Double
)