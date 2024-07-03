package com.example.weibo_lvruizhong.ui.login

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LoginManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("login", Context.MODE_PRIVATE)
    }

    fun login(phone: String, smsCode: String, callback: (Boolean) -> Unit) {
        val client = OkHttpClient()


        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            "{\"phone\":\"$phone\",\"smsCode\":\"$smsCode\"}"
        )

        val request = Request.Builder()
            .url("https://hotfix-service-prod.g.mi.com/weibo/api/auth/login")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LoginManager", "Login request failed", e)
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody)

                val code = jsonObject.getInt("code")
                if (code == 200) {
                    val token = jsonObject.getString("data")
                    val expirationTime = System.currentTimeMillis() / 1000 + 12 * 60 * 60
                    saveToken(token, expirationTime)
                    callback(true)
                } else {
                    callback(false)
                }
            }
        })
    }


    fun saveToken(token: String, expirationTime: Long) {
        sharedPreferences.edit()
            .putString("token", token)
            .putLong("expirationTime", expirationTime)
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("token", null)
    }

    fun clearToken() {
        sharedPreferences.edit().remove("token").apply()
    }

    fun isTokenExpired(): Boolean {
        // 获取当前时间戳
        val currentTime = System.currentTimeMillis() / 1000

        // 获取保存的 token 过期时间戳
        val expirationTime = sharedPreferences.getLong("expirationTime", 0)

        // 判断当前时间是否大于过期时间
        return currentTime > expirationTime
    }


}