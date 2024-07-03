package com.example.weibo_lvruizhong

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weibo_lvruizhong.databinding.ActivityLoginBinding
import com.example.weibo_lvruizhong.ui.login.LoginManager
import com.example.weibo_lvruizhong.ui.notifications.NotificationsFragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private var countDownTimer: CountDownTimer? = null
    private lateinit var binding: ActivityLoginBinding

    private lateinit var etPhoneNumber: EditText
    private lateinit var etCode: EditText
    private lateinit var btnGetCode: Button
    private lateinit var btnLogin: Button
    val phonemaxLength = 11
    val codemaxLength = 6
    var phonenumberok = false

    var id = 0;

    //输入手机号限制11位的数字
    val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val input = s?.toString()
            if (input?.length ?: 0 > phonemaxLength) {
                // 如果输入超过了最大位数，截取前面的11个字符
                val truncatedInput = input?.substring(0, phonemaxLength)
                etPhoneNumber.removeTextChangedListener(this) // 移除监听器，避免死循环
                etPhoneNumber.setText(truncatedInput)
                etPhoneNumber.setSelection(phonemaxLength)
                etPhoneNumber.addTextChangedListener(this) // 重新添加监听器
            }
        }

        override fun afterTextChanged(s: Editable?) {
            val input = s?.toString()
            if (input?.length == phonemaxLength) {
                // 允许获取验证码
                phonenumberok = true
            } else {
                // 不允许获取验证码
                phonenumberok = false
            }
            // 检查手机号和验证码是否都不为空
            val phone = etPhoneNumber.text.toString()
            val smsCode = etCode.text.toString()
            btnLogin.isEnabled = phone.isNotEmpty() && smsCode.isNotEmpty()

        }
    }

    //输入验证码限制6位的数字
    val textWatcher2 = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val input = s?.toString()

            if (input?.length ?: 0 > codemaxLength) {
                // 如果输入超过了最大位数，截取前面的11个字符
                val truncatedInput = input?.substring(0, codemaxLength)
                etCode.removeTextChangedListener(this) // 移除监听器，避免死循环
                etCode.setText(truncatedInput)
                etCode.setSelection(codemaxLength)
                etCode.addTextChangedListener(this) // 重新添加监听器
            }

        }

        override fun afterTextChanged(s: Editable?) {
            // 检查手机号和验证码是否都不为空
            val phone = etPhoneNumber.text.toString()
            val smsCode = etCode.text.toString()
            btnLogin.isEnabled = phone.isNotEmpty() && smsCode.isNotEmpty()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginManager = LoginManager(this)

        // 检查是否已保存Token
        val token = loginManager.getToken()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //初始化计时器
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 在倒计时过程中，更新按钮的文本显示
                btnGetCode.isEnabled = false
                btnGetCode.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                // 倒计时结束后，恢复按钮的可点击状态
                btnGetCode.isEnabled = true
                btnGetCode.text = "获取验证码"
            }
        }


        etPhoneNumber = findViewById(R.id.edit_phone)
        etCode = findViewById(R.id.edit_code)
        btnLogin = findViewById(R.id.btn_login)

        etPhoneNumber.addTextChangedListener(textWatcher)
        etCode.addTextChangedListener(textWatcher2)
        // 设置初始状态为不可点击
        btnLogin.isEnabled = false

        //获取验证码按钮的点击事件
        btnGetCode = findViewById(R.id.btn_get_code)
        btnGetCode.setOnClickListener {
            if (phonenumberok) {
                // 允许获取验证码
                // TODO: 在这里添加获取验证码的逻辑
                sendVerificationCode()
                // 启动倒计时
                countDownTimer?.start()
            } else {
                // 不允许获取验证码
                Toast.makeText(this@LoginActivity, "请输入完整的手机号", Toast.LENGTH_SHORT).show()
            }

        }

        // 设置登录按钮点击事件
        btnLogin.setOnClickListener {
            val phone = etPhoneNumber.text.toString()
            val smsCode = etCode.text.toString()

            // 检查验证码是否正确，如果正确则请求用户信息
            loginManager.login(phone, smsCode) { success ->
                if (success) {
                        getUserInfo(token.toString())
                } else {
                    // 登录失败，执行自己的逻辑
                    // TODO: 在这里添加登录失败的逻辑
                }
            }
        }
    }


    //获取验证码的网络请求函数
    private fun sendVerificationCode() {
        val phoneNumber = etPhoneNumber.text.toString()

        val url = "https://hotfix-service-prod.g.mi.com/weibo/api/auth/sendCode"
        val requestBody = JSONObject().apply {
            put("phone", phoneNumber)
        }.toString()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败的处理
                Log.e("请求失败","请求失败")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    val jsonObject = JSONObject(responseData)
                    val code = jsonObject.getInt("code")
                    val msg = jsonObject.getString("msg")
                    val data = jsonObject.getBoolean("data")

                    if (code == 200 && data) {
                        // 验证码发送成功的处理
                    } else {
                        // 验证码发送失败的处理
                    }
                } else {
                    // 请求失败的处理
                }
            }
        })
    }


    //请求用户信息函数
    private fun getUserInfo(token: String) {
        // 请求地址
        val url = "https://hotfix-service-prod.g.mi.com/weibo/api/user/info"

        // 创建请求对象
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", token)
            .get()
            .build()

        // 创建 OkHttpClient 对象
        val client = OkHttpClient()

        // 发送请求并处理响应
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败的处理
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    try {
                        val jsonObject = JSONObject(responseData)
                        val code = jsonObject.getInt("code")
                        if (code == 200) {
                            val data = jsonObject.getJSONObject("data")
                            if (!data.isNull("id")) {
                                val id = data.getInt("id")
                                // 进行后续操作
                            } else {
                                // 处理字段值为 null 的情况
                                val id = 0
                            }

                            val username = data.getString("username")
                            val phone = data.getString("phone")
                            val avatar = data.getString("avatar")
                            val loginStatus = data.getBoolean("loginStatus")

                            // 创建Bundle对象，将用户信息添加到Bundle中
                            val bundle = Bundle()
                            bundle.putInt("id", id)
                            bundle.putString("username", username)
                            bundle.putString("phone", phone)
                            bundle.putString("avatar", avatar)
                            bundle.putBoolean("loginStatus", loginStatus)

                            // 跳转回进入LoginActivity的fragment页面，并传递Bundle
                            goToFragmentPage(bundle)
                        } else {
                            // 请求失败的处理
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    //获取用户信息成功，返回”我的“界面
    private fun goToFragmentPage(bundle: Bundle) {
        val fragment = NotificationsFragment()
        fragment.arguments = bundle

        val intent = Intent(this, MainActivity::class.java)
        val bundlegive = bundle
        intent.putExtras(bundle)
        startActivity(intent)
    }

}