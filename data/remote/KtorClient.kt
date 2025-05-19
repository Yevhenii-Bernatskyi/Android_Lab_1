package com.example.android_lab_1.data.remote

import io.ktor.client.*
import io.ktor.client.engine.cio.* // Можна замінити на OkHttp або Android, якщо потрібно
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    // Створюємо екземпляр Json для налаштування серіалізації
    private val json = Json {
        prettyPrint = true // Для логування JSON у читабельному форматі
        isLenient = true // Дозволяє більш гнучкий парсинг (наприклад, рядки замість чисел, якщо можливо)
        ignoreUnknownKeys = true // Дуже важливо! Якщо API додасть нові поля, твій додаток не впаде.
        coerceInputValues = true // Намагається привести типи, наприклад, null до значення за замовчуванням для не-нульових типів (якщо вони мають значення за замовчуванням)
    }

    // Створюємо та налаштовуємо HTTP клієнт
    val httpClient = HttpClient(CIO) { // CIO - це один з двигунів Ktor. Для Android також популярні OkHttp або Android двигуни.

        // Плагін для обробки контенту (Content Negotiation)
        // Дозволяє автоматично перетворювати тіла запитів/відповідей в/з Kotlin об'єктів
        install(ContentNegotiation) {
            json(json) // Вказуємо, що будемо використовувати kotlinx.serialization для JSON
        }

        // Плагін для логування HTTP запитів та відповідей
        // Дуже корисно для відладки
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    // Використовуй Android Logcat для виведення логів
                    // Можна налаштувати тег для зручності фільтрації
                    android.util.Log.d("KtorLogger", message)
                }
            }
            level = LogLevel.ALL // Логувати все: заголовки, тіла, статуси тощо.
            // Для релізу можна змінити на LogLevel.INFO або LogLevel.NONE
        }

        // Тут можна додати інші налаштування клієнта, наприклад:
        // - defaultRequest для базового URL, спільних заголовків
        // - HttpTimeout для таймаутів
        // - Auth для аутентифікації (якщо потрібна)
    }
}