package com.example.weibo_lvruizhong.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.weibo_lvruizhong.LoginActivity
import com.example.weibo_lvruizhong.MainActivity
import com.example.weibo_lvruizhong.R
import com.example.weibo_lvruizhong.databinding.FragmentNotificationsBinding
import com.example.weibo_lvruizhong.ui.login.LoginManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class NotificationsFragment : Fragment() {
    var UserLoggedIn = false
    private lateinit var loginManager: LoginManager

private var _binding: FragmentNotificationsBinding? = null
  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
      val homeViewModel =
          ViewModelProvider(this).get(NotificationsViewModel::class.java)

      _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
      val root: View = binding.root


      // 初始化 LoginManager
      loginManager = LoginManager(requireContext())
      // 检查本地是否有 token
      val loginManager = context?.let { LoginManager(it) }
      val token = loginManager?.getToken()
      val imageview: ImageView = _binding!!.imageAvatar
      val textview: TextView = _binding!!.textName
      if (token != null) {
          // 检查 token 是否过期
          if (loginManager.isTokenExpired()) {
              // token 过期，删除本地 token
              loginManager.clearToken()
              // TODO: 在这里处理 token 过期的情况
          } else {
              // token 未过期，进行相应的操作
              // TODO: 在这里处理 token 未过期的情况
              getUserInfo2(token,imageview,textview)
          }
      } else {
          // 本地没有 token
          // TODO: 在这里处理没有 token 的情况
          //什么都不做
      }

      val bundle = arguments
      if (bundle != null) {
          UserLoggedIn = true
          val sharedPreferences = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE)
          val editor = sharedPreferences.edit()
          editor.putBoolean("isLoggedIn", true)
          editor.apply()
          val id = bundle.getInt("id")
          val username = bundle.getString("username")
          val phone = bundle.getString("phone")
          val avatar = bundle.getString("avatar")
          val loginStatus = bundle.getBoolean("loginStatus")

//          // 在页面上展示用户信息
//          // TODO: 在这里添加展示用户信息的逻辑
//          val imageview: ImageView = _binding!!.imageAvatar
//          Glide.with(requireContext())
//              .load(avatar) // Replace "avatar" with the actual URL or resource of the image
//              .placeholder(R.drawable.ic_like) // Replace "placeholder" with the resource ID of a placeholder image
////              .error(R.drawable.ic_delete) // Replace "error" with the resource ID of an error image
//              .into(imageview)
//          val textview: TextView = _binding!!.textName
//          textview.text = username
//          val textviewfan: TextView = _binding!!.textFans
//          textviewfan.text="粉丝："

      }

    if (!UserLoggedIn){
        val imageview: ImageView = _binding!!.imageAvatar
        imageview.setOnClickListener{
            if (UserLoggedIn) {
                // 用户已登录，不进行跳转
            } else {
                // 用户未登录，跳转到登录页面
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    return root
  }

    override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
    }

    private fun getUserInfo2(token: String,imageview: ImageView,textview: TextView) {
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
                                var id = data.getInt("id")
                                // 进行后续操作
                            } else {
                                // 处理字段值为 null 的情况
                                var id = 0
                            }
                            val username = data.getString("username")
                            val phone = data.getString("phone")
                            val avatar = data.getString("avatar")
                            val loginStatus = data.getBoolean("loginStatus")
                            (activity as MainActivity).runOnUiThread {
                                Glide.with(requireContext())
                                    .load(avatar) // Replace "avatar" with the actual URL or resource of the image
                                    .placeholder(R.drawable.logo) // Replace "placeholder" with the resource ID of a placeholder image
//                                    .error(R.drawable.ic_delete) // Replace "error" with the resource ID of an error image
                                    .into(imageview)
                                textview.setText(username)
                                val textviewfan: TextView = _binding!!.textFans
                                textviewfan.text="粉丝："
                            }

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
}
