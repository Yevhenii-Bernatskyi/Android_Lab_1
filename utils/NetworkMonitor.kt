package com.example.android_lab_1.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf // Для початкового значення, якщо потрібно

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isNetworkAvailable: Flow<Boolean> = callbackFlow {
        // Об'єкт для відстеження змін стану мережі
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // Мережа доступна, надсилаємо true
                trySend(true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                // Мережу втрачено, надсилаємо false
                trySend(false)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                // Мережа недоступна (рідкісний випадок, зазвичай спрацьовує onLost)
                trySend(false)
            }
        }

        // Реєструємо NetworkCallback для отримання оновлень
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Нас цікавить саме доступ до Інтернету
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Перевіряємо початковий стан мережі при підписці на Flow
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
        } else {
            trySend(false) // Якщо немає активної мережі
        }

        // Цей блок викликається, коли споживач Flow відписується (наприклад, ViewModel знищується)
        // Важливо відреєструвати callback, щоб уникнути витоків пам'яті.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // Випромінювати значення, тільки якщо воно змінилося,
    // щоб уникнути зайвих оновлень UI.
}