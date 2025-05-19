package com.example.android_lab_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android_lab_1.data.local.ForecastItemEntity
import com.example.android_lab_1.ui.theme.WeatherUiState // Переконайтесь, що шлях правильний
import com.example.android_lab_1.ui.theme.WeatherViewModel // Переконайтесь, що шлях правильний
import com.example.android_lab_1.ui.theme.Android_Lab_1Theme // Переконайтесь, що ця тема існує
import java.text.SimpleDateFormat
import java.util.* // Для Date


class MainActivity : ComponentActivity() {

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
        weatherViewModel.refreshWeatherData("Kyiv")
    }
}

@Composable
fun CurrentWeatherCard(weatherData: ForecastItemEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Зараз в ${weatherData.cityName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            val sdf = remember { SimpleDateFormat("HH:mm, EEEE, dd MMMM", Locale("uk", "UA")) }
            val dateString = remember(weatherData.dateTimeStamp) {
                sdf.format(Date(weatherData.dateTimeStamp * 1000L))
            }
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${weatherData.weatherDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp
            )
            // Можна додати іконку погоди тут
            // AsyncImage(
            //    model = "https://openweathermap.org/img/wn/${weatherData.weatherIcon}@4x.png",
            //    contentDescription = weatherData.weatherDescription,
            //    modifier = Modifier.size(96.dp)
            // )
            Text(
                text = "${weatherData.temp}°C",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Відчувається як: ${weatherData.feelsLike}°C",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Вологість: ${weatherData.humidity}%")
                Text("Тиск: ${weatherData.pressure} гПа")
            }
            Text(
                text = "Вітер: ${weatherData.windSpeed} м/с",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Для використання Scaffold та TopAppBar з Material 3
@Composable
fun WeatherAppScreen(viewModel: WeatherViewModel) {
    val uiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var forecastExpanded by remember { mutableStateOf(false) }


    LaunchedEffect(isNetworkAvailable, uiState) {
        if (!isNetworkAvailable && uiState !is WeatherUiState.Loading) {
            snackbarHostState.showSnackbar(
                message = "Немає підключення до мережі. Дані можуть бути застарілими.",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Прогноз погоди") }, // Оновлений заголовок
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column( // Використовуємо Column для основного компонування
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Загальні відступи
        ) {
            when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is WeatherUiState.Success -> {
                    state.currentWeatherData?.let { currentWeather ->
                        CurrentWeatherCard(weatherData = currentWeather)
                    } ?: Box(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            "Дані про поточну погоду недоступні.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Роздільник або відступ (опціонально)
                    // Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Секція для випадаючого списку прогнозу
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { forecastExpanded = !forecastExpanded }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Прогноз на найближчий час",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (forecastExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (forecastExpanded) "Згорнути" else "Розгорнути"
                            )
                        }

                        AnimatedVisibility(visible = forecastExpanded) {
                            if (state.forecastList.isNotEmpty()) {
                                // Тут ми помістимо ForecastLazyColumn всередину Box з вагою,
                                // щоб він правильно скролився, якщо контенту багато,
                                // а батьківський Column не скролиться.
                                Box(modifier = Modifier.weight(1f, fill = false)) {
                                    ForecastLazyColumn(forecastItems = state.forecastList)
                                }
                            } else {
                                Text(
                                    "Детальний прогноз на найближчий час відсутній.",
                                    modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
                is WeatherUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Помилка: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is WeatherUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Немає даних для відображення. Перевірте підключення до мережі та спробуйте оновити.",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastLazyColumn(forecastItems: List<ForecastItemEntity>) {
    // Цей Composable тепер буде використовуватися всередині AnimatedVisibility.
    // Переконайтеся, що він не має власного Modifier.fillMaxSize(),
    // якщо батьківський елемент вже контролює розмір.
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxHeight() // Дозволяє скролити, якщо вміст більший за доступне місце
    ) {
        items(forecastItems) { forecastItem ->
            ForecastItemRow(item = forecastItem)
        }
    }
}

@Composable
fun ForecastItemRow(item: ForecastItemEntity) {
    val sdf = remember { SimpleDateFormat("EEEE, dd MMMM, HH:mm", Locale("uk", "UA")) }
    val dateString = remember(item.dateTimeStamp) {
        sdf.format(Date(item.dateTimeStamp * 1000L))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Трохи менша тінь для елементів списку
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
                text = "${item.weatherDescription.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}",
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
                text = "Тиск: ${item.pressure} гПа",
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
                    color = Color(0xFF4A90E2) // Більш приємний синій
                )
            }
        }
    }
}