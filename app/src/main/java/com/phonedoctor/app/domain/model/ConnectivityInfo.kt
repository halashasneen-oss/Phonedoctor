package com.phonedoctor.app.domain.model

data class ConnectivityInfo(
    val wifiAvailable: Boolean,
    val wifiConnected: Boolean,
    val bluetoothAvailable: Boolean,
    val bluetoothEnabled: Boolean,
    val mobileNetworkAvailable: Boolean,
    val mobileNetworkConnected: Boolean,
    val internetReachable: Boolean?
)
