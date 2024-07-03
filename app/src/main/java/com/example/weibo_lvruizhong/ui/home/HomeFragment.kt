package com.example.weibo_lvruizhong.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weibo_lvruizhong.LoginActivity
import com.example.weibo_lvruizhong.databinding.FragmentHomeBinding
import com.example.weibo_lvruizhong.ui.home.Recycler.ImageSelectEvent
import com.example.weibo_lvruizhong.ui.home.Recycler.Post
import com.example.weibo_lvruizhong.ui.home.Recycler.PostAdapter
import com.example.weibo_lvruizhong.ui.home.Recycler.PostLikeChangeEvent
import com.example.weibo_lvruizhong.ui.login.LoginManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONObject

class HomeFragment : Fragment() {

    private lateinit var postRecyclerView: RecyclerView
    private lateinit var errorLayout: LinearLayout
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>
    private lateinit var loginManager: LoginManager
    private lateinit var retryButton: Button
    private var token: String? = null
    private var isRequestSuccessful = false
    private var isNoMoreData = false

private var _binding: FragmentHomeBinding? = null
  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    val root: View = binding.root

      retryButton = binding.retryButton

      // 初始化postRecyclerView
      postRecyclerView = binding.recyclerPosts
      errorLayout = binding.errorLayout

      // 初始化postList
      postList = mutableListOf<Post>()

      // 初始化 LoginManager
      loginManager = LoginManager(requireContext())


      if(hasSavedPostList()){
          showList()
          postAdapter = PostAdapter(loadPostListFromLocal())
          postRecyclerView.layoutManager = LinearLayoutManager(activity)
          postRecyclerView.adapter = postAdapter
      }else{
          showNetworkError()
          postAdapter = PostAdapter(postList)
          postRecyclerView.layoutManager = LinearLayoutManager(activity)
          postRecyclerView.adapter = postAdapter
      }

      // 注册EventBus
      EventBus.getDefault().register(this)

      val swipeRefreshLayout = binding.swipeRefreshLayout
      swipeRefreshLayout.setOnRefreshListener {
          // 在这里执行刷新操作
          // 例如，重新获取数据并更新RecyclerView
          // 检查本地是否有 token
          istokenuseful()
          GetPostDate(token.toString())
          postAdapter = PostAdapter(loadPostListFromLocal())
          postRecyclerView.adapter = postAdapter
          // 刷新完成后，调用setRefreshing(false)来停止刷新动画
          swipeRefreshLayout.isRefreshing = false
      }
      postRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
              super.onScrolled(recyclerView, dx, dy)
              val layoutManager = recyclerView.layoutManager as LinearLayoutManager
              val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
              swipeRefreshLayout.isEnabled = firstVisibleItemPosition == 0

              val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
              val totalItemCount = layoutManager.itemCount
              if (lastVisibleItemPosition == totalItemCount - 1 && !isNoMoreData) {
                  // 到达列表底部且还有更多数据
                  // 在这里执行加载更多的操作
                  // 例如，可以调用一个方法来加载下一页数据
                  // LoadMoreData()
                  // 如果没有更多数据，则将isNoMoreData设置为true
                  // isNoMoreData = true
                  // 弹出"无更多内容"的提示
                  Toast.makeText(requireContext(), "无更多内容", Toast.LENGTH_SHORT).show()
              }
          }
      })
    return root
  }

    override fun onDestroyView() {
            super.onDestroyView()
            _binding = null

        // 取消注册EventBus
        EventBus.getDefault().unregister(this)
    }


    fun GetPostDate(token:String){
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://hotfix-service-prod.g.mi.com/weibo/homePage?current=1&size=10")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", token.toString())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败处理
                Log.e("error","请求失败")
                isRequestSuccessful = false
                // 显示网络请求失败页面
                showNetworkError()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                // 解析JSON数据
                val jsonObject = JSONObject(responseData)
                val dataArray = jsonObject.getJSONObject("data").getJSONArray("records")
                if (dataArray.length() == 0) {
                    isNoMoreData = true
                }
                // 遍历数据数组并将每个对象转换为Post对象
                val tempList  = mutableListOf<Post>()
                for (i in 0 until dataArray.length()) {
                    val dataObj = dataArray.getJSONObject(i)
                    val imagesArray = dataObj.optJSONArray("images") ?: JSONArray() // 检查"images"字段是否为null，如果是null则设置为空的JSONArray
                    val post = Post(
                        dataObj.getInt("id"),
                        dataObj.getInt("userId"),
                        dataObj.getString("username"),
                        dataObj.getString("phone"),
                        dataObj.getString("avatar"),
                        dataObj.getString("title"),
                        dataObj.getString("videoUrl"),
                        dataObj.getString("poster"),
                        // 解析图片数组
                        parseImages(imagesArray),
                        dataObj.getInt("likeCount"),
                        dataObj.getBoolean("likeFlag"),
                        dataObj.getString("createTime")
                    )
                    tempList .add(post)
                }
                // 将postList保存到你的postlist中
                // 例如，可以使用addAll方法将tempList添加到postlist中
                isRequestSuccessful = true
                Log.v("msg","获取Post成功")
                // 显示列表页
                //showList()
                // 通知适配器数据发生了变化
                activity?.runOnUiThread {
                    errorLayout.visibility = View.GONE
                    postRecyclerView.visibility = View.VISIBLE
                    postList.clear()
                    postList.addAll(tempList)
                    savePostListToLocal(tempList)
                    postAdapter.notifyDataSetChanged()
                }
            }
        })
    }
    // 解析图片数组的辅助函数
    fun parseImages(jsonArray: JSONArray): MutableList<String> {
        val images = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            images.add(jsonArray.getString(i))
        }
        return images
    }
    private fun showNetworkError() {
        postRecyclerView.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
        // 显示网络请求失败的提示和重试按钮
        // 例如，可以将errorTextView和retryButton设置为可见
        retryButton.setOnClickListener {
            // 在这里重新发起网络请求
            // 例如，可以调用一个方法来重新获取数据
            GetPostDate(token.toString())
        }
    }

    private fun showList() {
        activity?.runOnUiThread {
            errorLayout.visibility = View.GONE
            postRecyclerView.visibility = View.VISIBLE
            // 隐藏网络请求失败的提示和重试按钮
            // 例如，可以将errorTextView和retryButton设置为不可见
            // 更新RecyclerView的数据
            fun updateData(newPostList: List<Post>) {
                postList.clear()
                postList.addAll(newPostList)
                postAdapter.notifyDataSetChanged()
            }
            //updateData(postList)
            Log.v("msg","更新postlist")
        }
    }
    fun istokenuseful() {
        val loginManager = context?.let { LoginManager(it) }
        token = loginManager?.getToken()

        if (token != null) {
            // 检查 token 是否过期
            if (loginManager != null) {
                if (loginManager.isTokenExpired()) {
                    // token 过期，删除本地 token
                    loginManager.clearToken()
                    // TODO: 在这里处理 token 过期的情况
                    // 跳转到登录页
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    // token 未过期，进行相应的操作
                    // TODO: 在这里处理 token 未过期的情况
                    //GetPostDate(token.toString())
                }
            }
        } else {
            // 本地没有 token
            // TODO: 在这里处理没有 token 的情况
            //什么都不做
            // 跳转到登录页
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    // 保存postList到本地的方法
    private fun savePostListToLocal(postList: MutableList<Post>) {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(postList)
        editor.putString("postList", json)
        editor.apply()
    }

    // 判断本地是否有保存的postList
    private fun hasSavedPostList(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.contains("postList")
    }
    // 从本地加载postList的方法
    private fun loadPostListFromLocal(): MutableList<Post> {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("postList", null)
        val gson = Gson()
        val type = object : TypeToken<MutableList<Post>>() {}.type
        return gson.fromJson(json, type)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostLikeChangeEvent(event: PostLikeChangeEvent) {
// 更新本地保存的postList

        val postList = loadPostListFromLocal()
        Log.d("PostLikeChangeEvent", "Loaded postList: $postList")

        val index = postList.indexOfFirst { post -> post.id == event.post.id }
        if (index != -1) {
            postList[index] = event.post
            savePostListToLocal(postList)
            Log.d("PostLikeChangeEvent", "Saved postList: $postList")
        }
    }
}