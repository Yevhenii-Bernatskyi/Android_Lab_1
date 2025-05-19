package com.example.android_lab_1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Анотація @Database визначає клас як базу даних Room.
// entities - список усіх Entity класів, які є частиною цієї БД.
// version - версія БД. Якщо ти змінюєш схему БД (додаєш таблиці, колонки), потрібно збільшити версію
//           і надати міграцію, або використати fallbackToDestructiveMigration для простоти.
// exportSchema = false - відключає експорт схеми БД в JSON файл (для лабораторної це не критично).
@Database(
    entities = [ForecastItemEntity::class, LastFetchTimestampEntity::class],
    version = 1, // Початкова версія
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Абстрактний метод, який повертає екземпляр WeatherDao.
    // Room автоматично згенерує реалізацію цього методу.
    abstract fun weatherDao(): WeatherDao

    // Companion object для реалізації синглтона бази даних.
    // Це гарантує, що у всьому додатку буде лише один екземпляр БД,
    // що є ефективним та запобігає проблемам.
    companion object {
        // @Volatile гарантує, що значення INSTANCE завжди актуальне для всіх потоків.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // synchronized гарантує, що лише один потік може одночасно виконувати цей блок коду,
            // запобігаючи створенню кількох екземплярів БД.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Використовуй applicationContext, щоб уникнути витоків пам'яті
                    AppDatabase::class.java,
                    "weather_app_database" // Назва файлу бази даних
                )
                    // Для лабораторної роботи, якщо ти будеш змінювати схему (наприклад, додавати колонки),
                    // найпростіший спосіб - це дозволити Room руйнувати та перебудовувати БД.
                    // УВАГА: Це видалить всі дані при оновленні схеми!
                    // Для реальних додатків потрібно реалізовувати міграції (Migrations).
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // Повертаємо створений екземпляр
                instance
            }
        }
    }
}