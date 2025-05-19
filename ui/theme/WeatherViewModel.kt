package com.example.android_lab_1.ui.theme

import android.app.Application // Потрібен для AndroidViewModel, щоб отримати Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_lab_1.data.WeatherRepository
import com.example.android_lab_1.data.local.AppDatabase
import com.example.android_lab_1.data.local.ForecastItemEntity
import com.example.android_lab_1.data.remote.WeatherApiService
import com.example.android_lab_1.utils.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

// Клас для представлення стану UI погоди
sealed class WeatherUiState {
    object Loading : WeatherUiState() // Стан завантаження
    data class Success(val forecast: List<ForecastItemEntity>) : WeatherUiState() // Успішне завантаження даних
    data class Error(val message: String) : WeatherUiState() // Помилка
    object Empty : WeatherUiState() // Якщо дані не завантажені і немає помилки (наприклад, кеш порожній, мережі немає)
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    // Ініціалізація залежностей
    // В ідеалі це робиться через Dependency Injection (Hilt, Koin),
    // але для лабораторної можна і так.
    private val weatherDao = AppDatabase.getDatabase(application).weatherDao()
    private val weatherApiService = WeatherApiService() // Використовує KtorClient.httpClient за замовчуванням
    private val networkMonitor = NetworkMonitor(application)
    private val repository = WeatherRepository(weatherApiService, weatherDao, networkMonitor)

    // MutableStateFlow для внутрішнього зберігання стану UI
    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    // StateFlow, який буде спостерігатися з UI (Composable)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    // StateFlow для стану мережі, який може спостерігатися з UI
    val isNetworkAvailable: StateFlow<Boolean> = networkMonitor.isNetworkAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Починати збір, коли є активні підписники, і тримати 5с
            initialValue = true // Початкове значення (можна отримати поточний стан при ініціалізації)
        )

    // Початкове завантаження даних при ініціалізації ViewModel
    // init {
    //     fetchWeatherForecast("Kyiv") // Або місто за замовчуванням
    // }

    /**
     * Завантажує прогноз погоди для вказаного міста.
     * @param cityName Назва міста.
     * @param forceRefresh Якщо true, дані будуть завантажені з мережі, ігноруючи кеш.
     */
    fun fetchWeatherForecast(cityName: String, forceRefresh: Boolean = false) {
        // Встановлюємо стан завантаження
        _weatherUiState.value = WeatherUiState.Loading

        viewModelScope.launch {
            try {
                repository.getForecast(cityName, forceRefresh)
                    .catch { exception ->
                        // Обробка помилок, що прийшли з репозиторію
                        _weatherUiState.value = WeatherUiState.Error(
                            exception.message ?: "Невідома помилка завантаження"
                        )
                    }
                    .collect { forecastList ->
                        if (forecastList.isNotEmpty()) {
                            _weatherUiState.value = WeatherUiState.Success(forecastList)
                        } else {
                            // Якщо список порожній, але не було помилки від репозиторію,
                            // це може означати, що кеш порожній і мережі не було / не вдалося завантажити.
                            // Перевіряємо, чи поточний стан вже не є помилкою, щоб не перезаписати її.
                            if (_weatherUiState.value !is WeatherUiState.Error) {
                                _weatherUiState.value = WeatherUiState.Empty
                            }
                        }
                    }
            } catch (e: IOException) { // Ловимо IOException, які може кинути репозиторій
                _weatherUiState.value = WeatherUiState.Error(e.message ?: "Помилка вводу/виводу")
            } catch (e: Exception) { // Загальна обробка інших непередбачених помилок
                _weatherUiState.value = WeatherUiState.Error(e.message ?: "Сталася непередбачена помилка")
            }
        }
    }

    /**
     * Метод для оновлення даних, який можна викликати, наприклад, з onStart() Activity.
     * Репозиторій сам вирішить, чи потрібно робити запит до API, чи використовувати кеш.
     */
    fun refreshWeatherData(cityName: String = "Kyiv") { // Можна передавати місто
        // Викликаємо fetchWeatherForecast з forceRefresh = false,
        // щоб спочатку перевірити кеш відповідно до його терміну дії.
        // Якщо потрібно гарантовано оновити з мережі (наприклад, по pull-to-refresh),
        // то forceRefresh можна встановити в true (після перевірки мережі).
        fetchWeatherForecast(cityName, forceRefresh = false)
    }
}