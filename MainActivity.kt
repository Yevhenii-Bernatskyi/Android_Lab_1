package com.example.android_lab_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels // Для делегата by viewModels()
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Важливо для безпечного збору Flow
import com.example.android_lab_1.data.local.ForecastItemEntity
import com.example.android_lab_1.ui.theme.WeatherUiState
import com.example.android_lab_1.ui.theme.WeatherViewModel
import com.example.android_lab_1.ui.theme.Android_Lab_1Theme // Переконайся, що ця тема існує
import java.text.SimpleDateFormat
import java.util.* // Для Date
import java.util.concurrent.TimeUnit // Для форматування дати (якщо потрібно)

class MainActivity : ComponentActivity() {

    // Отримуємо екземпляр WeatherViewModel за допомогою делегата by viewModels()
    private val weatherViewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Android_Lab_1Theme { // Застосовуємо загальну тему додатку
                WeatherAppScreen(viewModel = weatherViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Викликаємо оновлення даних при старті Activity.
        // ViewModel та Repository самі вирішать, чи потрібен новий запит до API.
        weatherViewModel.refreshWeatherData("Kyiv") // Можеш зробити місто динамічним
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Для використання Scaffold та TopAppBar з Material 3
@Composable
fun WeatherAppScreen(viewModel: WeatherViewModel) {
    // Збираємо стан UI та стан мережі з ViewModel, безпечно для життєвого циклу
    val uiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()

    // Створюємо стан для Snackbar (повідомлення)
    val snackbarHostState = remember { SnackbarHostState() }

    // Показуємо Snackbar, якщо мережа недоступна і ми не в стані завантаження
    // (щоб не показувати одночасно з індикатором завантаження, якщо запит вже йде)
    LaunchedEffect(isNetworkAvailable, uiState) {
        if (!isNetworkAvailable && uiState !is WeatherUiState.Loading) {
            snackbarHostState.showSnackbar(
                message = "Немає підключення до мережі. Дані можуть бути застарілими.",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Додаємо хост для Snackbar
        topBar = {
            TopAppBar(
                title = { Text("Прогноз Погоди в Києві") }, // Можна зробити динамічним
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding -> // Відступи, що надаються Scaffold для контенту
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Застосовуємо внутрішні відступи
                .padding(16.dp) // Додаткові відступи для контенту
        ) {
            when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    // Показуємо індикатор завантаження по центру
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WeatherUiState.Success -> {
                    // Якщо дані успішно завантажені, показуємо список
                    ForecastLazyColumn(forecastItems = state.forecast)
                }
                is WeatherUiState.Error -> {
                    // Якщо сталася помилка, показуємо повідомлення про помилку
                    Text(
                        text = "Помилка: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is WeatherUiState.Empty -> {
                    // Якщо дані порожні (кеш порожній і не вдалося завантажити)
                    Text(
                        text = "Немає даних для відображення. Перевірте підключення до мережі та спробуйте оновити.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastLazyColumn(forecastItems: List<ForecastItemEntity>) {
    if (forecastItems.isEmpty()) {
        // Цей випадок вже обробляється у WeatherAppScreen через WeatherUiState.Empty,
        // але можна залишити для ясності або якщо цей Composable буде перевикористовуватися.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Немає доступних даних прогнозу.")
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp) // Відстань між елементами списку
    ) {
        items(forecastItems) { forecastItem -> // `items` - це функція для LazyColumn
            ForecastItemRow(item = forecastItem)
        }
    }
}

@Composable
fun ForecastItemRow(item: ForecastItemEntity) {
    // Форматуємо дату та час
    // dt_timestamp - це Unix timestamp в секундах, Date() очікує мілісекунди
    val sdf = remember { SimpleDateFormat("EEEE, dd MMMM, HH:mm", Locale("uk", "UA")) }
    val dateString = remember(item.dateTimeStamp) {
        sdf.format(Date(item.dateTimeStamp * 1000L))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${item.weatherDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}", // Перша літера велика
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Температура: ${item.temp}°C",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Відчувається як: ${item.feelsLike}°C",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Вологість: ${item.humidity}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Тиск: ${item.pressure} гПа", // гПа - гектопаскалі
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Вітер: ${item.windSpeed} м/с",
                style = MaterialTheme.typography.bodyMedium
            )
            if (item.rainVolume3h != null && item.rainVolume3h > 0) {
                Text(
                    text = "Опади (3г): ${item.rainVolume3h} мм",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue // Просто для виділення
                )
            }
            // Тут можна додати іконку погоди, якщо завантажити їх або використати бібліотеку іконок
            // Наприклад, AsyncImage з Coil: // implementation("io.coil-kt:coil-compose:2.6.0")
            // AsyncImage(
            //    model = "https://openweathermap.org/img/wn/${item.weatherIcon}@2x.png",
            //    contentDescription = item.weatherDescription,
            //    modifier = Modifier.size(48.dp)
            // )
        }
    }
}