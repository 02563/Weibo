package com.example.weibo_lvruizhong

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog

class SplashActivity : AppCompatActivity() {
    private val SPLASH_DELAY: Long = 1000 // 延迟时间，单位为毫秒
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 检查是否需要显示声明与使用条款的弹窗
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val agreed = sharedPreferences.getBoolean("agreed", false)

        if (!agreed) {
            // 显示声明与使用条款的弹窗
            showTermsDialog()
        } else {
            // 用户已同意，直接进入主页
            startMainActivity()
        }
    }
    private fun showTermsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("声明与使用条款")
        builder.setMessage("这里是声明与使用条款的内容")

        builder.setPositiveButton("同意") { dialog, which ->
            // 用户同意，保存同意状态到SharedPreferences
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("agreed", true)
            editor.apply()

            // 启动主页
            startMainActivity()
        }

        builder.setNegativeButton("不同意") { dialog, which ->
            // 用户不同意，退出应用
            finish()
        }

        builder.setCancelable(false)
        builder.show()
    }
    private fun startMainActivity() {
        // 使用Handler延迟启动MainActivity
        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish() // 结束当前的SplashActivity
        }, SPLASH_DELAY)
    }
}