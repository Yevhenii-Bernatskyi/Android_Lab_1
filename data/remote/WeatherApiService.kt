package com.example.android_lab_1.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class WeatherApiService(
    // Приймаємо HttpClient як залежність (Dependency Injection)
    // За замовчуванням використовуємо наш KtorClient.httpClient
    private val httpClient: HttpClient = KtorClient.httpClient
) {

    // ЗАМІНИ "YOUR_OPENWEATHERMAP_API_KEY" НА СВІЙ СПРАВЖНІЙ КЛЮЧ!
    // Краще винести ключ в buildConfigField або local.properties для безпеки,
    // але для лабораторної тимчасово можна так.
    private val apiKey = "4e6b3e483f3fd5710bd03515280440c2"

    companion object {
        // Базовий URL для API прогнозу
        private const val BASE_URL_FORECAST = "https://api.openweathermap.org/data/2.5/forecast"
    }

    // Функція для отримання 5-денного/3-годинного прогнозу
    // suspend - бо це асинхронна операція, яка буде виконуватися в корутині
    suspend fun getFiveDayForecast(cityName: String): WeatherApiResponseDto {
        // Виконуємо GET-запит
        return httpClient.get(BASE_URL_FORECAST) {
            // Додаємо параметри запиту
            parameter("q", cityName)
            parameter("appid", apiKey)
            parameter("units", "metric") // Температура в Цельсіях
            parameter("lang", "ua")      // Мова відповіді (українська)
        }.body() // Автоматично розпарсить JSON відповідь у наш WeatherApiResponseDto
        // завдяки ContentNegotiation та kotlinx.serialization
    }

    // Тут можна додати інші функції для інших ендпоінтів API, якщо потрібно
    // наприклад, для отримання поточної погоди.
}