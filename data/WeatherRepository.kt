package com.example.android_lab_1.data

import android.content.Context // Потрібен для перевірки мережі, але краще передавати NetworkMonitor
import com.example.android_lab_1.data.local.LastFetchTimestampEntity
import com.example.android_lab_1.data.local.WeatherDao
import com.example.android_lab_1.data.remote.WeatherApiService
import com.example.android_lab_1.data.remote.WeatherApiResponseDto
import com.example.android_lab_1.data.local.ForecastItemEntity // Імпорт Entity
import com.example.android_lab_1.utils.NetworkMonitor // Імпортуємо NetworkMonitor, який створимо пізніше
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.io.IOException // Для обробки мережевих помилок

// Репозиторій відповідає за надання даних ViewModel.
// Він абстрагує джерела даних (мережа, база даних).
class WeatherRepository(
    private val weatherApiService: WeatherApiService,
    private val weatherDao: WeatherDao,
    private val networkMonitor: NetworkMonitor // Передаємо NetworkMonitor для перевірки мережі
) {

    // Часовий інтервал для кешування даних (наприклад, 1 година в мілісекундах)
    // Ти можеш налаштувати цей час відповідно до вимог.
    private val CACHE_EXPIRATION_TIME_MS = 1 * 60 * 60 * 1000L // 1 година

    /**
     * Отримує прогноз погоди для вказаного міста.
     * Спочатку перевіряє кеш (БД). Якщо дані в кеші застаріли, відсутні,
     * або якщо потрібне примусове оновлення (forceRefresh = true) і є мережа,
     * то завантажує дані з API, оновлює кеш і повертає дані з кешу.
     *
     * @param cityName Назва міста.
     * @param forceRefresh Якщо true, спробує завантажити дані з мережі, навіть якщо кеш не застарів.
     * @return Flow<List<ForecastItemEntity>> Потік з прогнозом погоди.
     *         Випромінює помилку через кидання винятку, якщо щось пішло не так.
     */
    fun getForecast(cityName: String, forceRefresh: Boolean = false): Flow<List<ForecastItemEntity>> = flow {
        // Спочатку випромінюємо дані з кешу, якщо вони є
        val cachedForecast = weatherDao.getForecastByCityOnce(cityName)
        if (cachedForecast.isNotEmpty()) {
            emit(cachedForecast)
        }

        // Перевіряємо, чи потрібно завантажувати дані з мережі
        val lastFetchTime = weatherDao.getLastFetchTimestamp(cityName)?.timestamp ?: 0L
        val currentTime = System.currentTimeMillis()
        val isCacheExpired = (currentTime - lastFetchTime) > CACHE_EXPIRATION_TIME_MS
        val isNetworkAvailable = networkMonitor.isNetworkAvailable.firstOrNull() ?: false // Отримуємо поточний стан мережі

        val shouldFetchFromNetwork = forceRefresh || isCacheExpired || cachedForecast.isEmpty()

        if (shouldFetchFromNetwork) {
            if (isNetworkAvailable) {
                try {
                    // Завантажуємо дані з API
                    val remoteData = weatherApiService.getFiveDayForecast(cityName)
                    // Мапуємо DTO в Entity
                    val forecastEntities = mapApiResponseToEntities(remoteData, cityName) // Використовуємо cityName з відповіді API

                    // Оновлюємо базу даних
                    weatherDao.deleteForecastByCity(remoteData.city.name) // Видаляємо старі дані для цього міста
                    weatherDao.insertForecastItems(forecastEntities)
                    weatherDao.updateLastFetchTimestamp(
                        LastFetchTimestampEntity(
                            cityNameKey = remoteData.city.name,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    // Після оновлення БД, випромінюємо оновлені дані
                    // Flow від weatherDao.getForecastByCity(remoteData.city.name) автоматично це зробить,
                    // але ми можемо випромінити поточний список, щоб UI оновився швидше,
                    // якщо він підписаний на цей flow { ... }
                    emit(weatherDao.getForecastByCityOnce(remoteData.city.name))

                } catch (e: Exception) {
                    // Обробка помилок мережі або парсингу
                    // Якщо кеш порожній і сталася помилка, потрібно повідомити про це
                    if (cachedForecast.isEmpty()) {
                        throw IOException("Помилка завантаження даних: ${e.localizedMessage}", e)
                    }
                    // Якщо кеш не порожній, UI продовжить показувати старі дані,
                    // а помилку можна залогувати або показати неблокуючим способом.
                    // Тут ми просто прокидаємо виняток, щоб ViewModel міг його обробити.
                    // Або можна не кидати, якщо хочемо просто мовчки використовувати кеш.
                    // Для лабораторної краще кинути, щоб показати обробку помилок.
                    e.printStackTrace() // Логуємо помилку
                    // Можна не кидати виняток, якщо хочемо, щоб UI показував кешовані дані,
                    // а про помилку повідомити іншим способом (наприклад, через окремий StateFlow у ViewModel)
                    // throw IOException("Помилка завантаження даних, використовуються кешовані: ${e.localizedMessage}", e)
                }
            } else {
                // Мережа недоступна, а потрібно оновити
                if (cachedForecast.isEmpty()) {
                    throw IOException("Мережа недоступна, і немає кешованих даних.")
                }
                // Якщо кеш є, UI буде його використовувати. Можна додати повідомлення користувачу.
            }
        }
        // Якщо не оновлювали з мережі, Flow від DAO (якщо ViewModel підписаний на нього)
        // вже надає актуальні дані з кешу.
        // Якщо ViewModel підписаний на цей flow {...}, то він вже отримав cachedForecast.
    }


    // Допоміжна функція для мапінгу відповіді API (DTO) в список сутностей БД (Entity)
    private fun mapApiResponseToEntities(apiResponse: WeatherApiResponseDto, defaultCityName: String): List<ForecastItemEntity> {
        val cityName = apiResponse.city.name.ifEmpty { defaultCityName } // Використовуємо назву міста з API, або передану
        val countryCode = apiResponse.city.country

        return apiResponse.list.map { forecastItemDto ->
            ForecastItemEntity(
                dateTimeText = forecastItemDto.dateTimeText,
                dateTimeStamp = forecastItemDto.dt,
                temp = forecastItemDto.main.temp,
                feelsLike = forecastItemDto.main.feelsLike,
                humidity = forecastItemDto.main.humidity,
                pressure = forecastItemDto.main.pressure,
                weatherMain = forecastItemDto.weather.firstOrNull()?.main ?: "N/A",
                weatherDescription = forecastItemDto.weather.firstOrNull()?.description ?: "N/A",
                weatherIcon = forecastItemDto.weather.firstOrNull()?.icon ?: "",
                windSpeed = forecastItemDto.wind.speed,
                cloudinessPercent = forecastItemDto.clouds.all,
                rainVolume3h = forecastItemDto.rain?.threeHourVolume, // Обережно з null
                cityName = cityName,
                countryCode = countryCode
            )
        }
    }
}