package com.di.refaliente.shared

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectionHelper {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        const val NONE = 0
        const val MOBILE_DATA = 1
        const val WIFI = 2
        const val VPN = 3

        fun getConnectionType(context: Context): Int {
            var result = NONE

            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let { cm ->
                cm.getNetworkCapabilities(cm.activeNetwork)?.let { nc ->
                    when {
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> { result = WIFI }
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> { result = MOBILE_DATA }
                        nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> { result = VPN }
                    }
                }
            }

            return result
        }
    }
}