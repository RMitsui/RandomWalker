package jp.ac.titech.itpro.sdl.randomwalkerpast

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object HttpClient {
    val instance = OkHttpClient()
}

class HttpUtil {
    fun httpGet(url : String): String? {
        val request = Request.Builder()
            .url(url)
            .build()

        var cp = HttpClient.instance.newBuilder().readTimeout(1, TimeUnit.MINUTES).build()

        val response = cp.newCall(request).execute()
        return response.body?.string()
    }
}