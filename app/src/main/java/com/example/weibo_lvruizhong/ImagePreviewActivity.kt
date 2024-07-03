package com.example.weibo_lvruizhong

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.weibo_lvruizhong.ui.ImageBrowse.ImagePagerAdapter
import com.example.weibo_lvruizhong.ui.home.Recycler.ImageSelectEvent
import com.example.weibo_lvruizhong.ui.home.Recycler.Post
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var selectpost: Post
    private var selectimage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)

        // 注册EventBus订阅者
        EventBus.getDefault().register(this)
        // 尝试获取之前发布的粘性事件
        val stickyEvent = EventBus.getDefault().getStickyEvent(
            ImageSelectEvent::class.java
        )
        if (stickyEvent != null) {
            // 如果有粘性事件，则处理它
            handleImageSelectEvent(stickyEvent)
            // 清理粘性事件（可选，取决于您是否需要再次获取它）
            EventBus.getDefault().removeStickyEvent(stickyEvent)
        }

        val downloadButton: Button = findViewById(R.id.downloadButton)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageNumber(position)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消注册EventBus订阅者
        EventBus.getDefault().unregister(this)
    }

    private fun handleImageSelectEvent(event: ImageSelectEvent) {
        // 处理事件逻辑
        selectpost = event.post
        selectimage = event.string

        val avatarImageView: ImageView = findViewById(R.id.avatarImageView)
        val nicknameTextView: TextView = findViewById(R.id.nicknameTextView)

        val avatarUrl = selectpost.avatar // 假设头像的URL存储在selectpost对象中
        val nickname = selectpost.username // 假设昵称存储在selectpost对象中

        Glide.with(this).load(avatarUrl).into(avatarImageView)

        nicknameTextView.text = nickname
        // 更新图片适配器的数据
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val adapter = ImagePagerAdapter(selectpost.images)
        viewPager.adapter = adapter

        // 更新当前页数的显示
        updatePageNumber(0)
    }

    private fun updatePageNumber(position: Int) {
        val pageNumberTextView: TextView = findViewById(R.id.pageCountTextView)

        val pageNumber = position + 1
        val totalPages = selectpost.images.size
        val pageNumberText = "$pageNumber / $totalPages"
        // 更新当前页数的显示
        pageNumberTextView.text = pageNumberText
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onImageSelectEvent(event: ImageSelectEvent) {
        handleImageSelectEvent(event)
    }
}