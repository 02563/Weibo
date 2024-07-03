package com.example.weibo_lvruizhong.ui.home.Recycler

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.example.weibo_lvruizhong.ImagePreviewActivity
import com.example.weibo_lvruizhong.LoginActivity
import com.example.weibo_lvruizhong.R
import com.example.weibo_lvruizhong.ui.login.LoginManager
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.IOException
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.Serializable


class PostAdapter(private val posts: MutableList<Post>) : BaseQuickAdapter<Post, PostAdapter.PostViewHolder>(R.layout.item_post, posts) {
    private var token: String? = null
    private var clickImage: String? = null
    private lateinit var selectedPost: Post
    override fun convert(holder: PostViewHolder, item: Post) {
        holder.bind(item)
    }

    inner class PostViewHolder(itemView: View) : BaseViewHolder(itemView) {
        private val avatarImageView: ImageView = itemView.findViewById(R.id.image_avatar)
        private val usernameTextView: TextView = itemView.findViewById(R.id.text_username)
        private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
        private val mediaRecyclerView: RecyclerView = itemView.findViewById(R.id.recycler_media)
        private val likeButton: ImageButton = itemView.findViewById(R.id.button_like)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
        private val commentsButton: ImageButton = itemView.findViewById(R.id.comments_button)

        fun bind(post: Post) {
            selectedPost = post // 给全局变量赋值
            // 设置头像
            Glide.with(itemView.context)
                .load(post.avatar)
                .placeholder(R.drawable.default_avatar)
                .into(avatarImageView)

            // 设置用户名
            usernameTextView.text = post.username

            titleTextView.text = post.title
            // 设置标题
            titleTextView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    titleTextView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val maxLines = 6
                    val layout = titleTextView.layout
                    if (layout != null) {
                        val lineCount = layout.lineCount
                        if (lineCount > maxLines) {
                            val start = layout.getLineStart(0)
                            val end = layout.getLineEnd(maxLines)
                            val truncatedTitle = if (end <= post.title.length) {
                                post.title.substring(start, end)
                            } else {
                                post.title
                            }
                            titleTextView.text = truncatedTitle + "..."
                        } else {
                            titleTextView.text = post.title
                        }
                    }
                }
            })
            // 测量titleTextView的尺寸
            titleTextView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            // 设置媒体
            if (post.images.size > 1) {
                val layoutManager = GridLayoutManager(itemView.context, 3)
                mediaRecyclerView.layoutManager = layoutManager
                val adapter = MediaAdapter(post.images, post.videourl)
                mediaRecyclerView.adapter = adapter
            } else {
                val layoutManager = GridLayoutManager(itemView.context, 1)
                mediaRecyclerView.layoutManager = layoutManager
                var adapter = MediaAdapter(post.images, post.videourl)
                if (post.videourl != "null") {
                    val nullimageUrls: MutableList<String> = mutableListOf(
                        "https://example.com/video1.mp4",
                    )
                    adapter = MediaAdapter(nullimageUrls, post.videourl)
                }
                mediaRecyclerView.adapter = adapter
            }

            // 设置点赞按钮
            likeButton.setImageResource(if (post.likeFlag) R.drawable.ic_like else R.drawable.ic_like_filled)
            likeButton.setOnClickListener {
                istokenuseful()
                if (token != null && !post.likeFlag) {
                    likePost(post)
                }
                else if (token != null && post.likeFlag) {
                    unlikePost(post)
                } else {
                    val intent = Intent(itemView.context, LoginActivity::class.java)
                    itemView.context.startActivity(intent)
                }
            }

                // 设置评论按钮点击事件
                commentsButton.setOnClickListener {
                    val position = adapterPosition + 1
                    Toast.makeText(
                        itemView.context,
                        "点击第${position}条数据评论按钮",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 设置删除按钮点击事件
                deleteButton.setOnClickListener {
                    removeItem(adapterPosition)
                }

        }


        inner class MediaAdapter(
            private val images: MutableList<String>,
            private val videoUrl: String?
        ) : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_media, images) {
            private val spacing = 5 // 行间距和列间距的大小，可以根据需要进行调整
            private val columnCount = 3 // 每行显示的图片数量，可以根据需要进行调整
            private val rowCount = 3 // 每列显示的图片数量，可以根据需要进行调整


            override fun convert(holder: BaseViewHolder, item: String) {
                val layoutParams = holder.itemView.layoutParams as GridLayoutManager.LayoutParams
                val position = holder.adapterPosition
                val column = position % columnCount
                val row = position / columnCount

                val horizontalSpacing = spacing / 2
                val verticalSpacing = spacing * 2

                layoutParams.setMargins(
                    if (column == 0) spacing else horizontalSpacing,
                    if (row == 0) spacing else verticalSpacing,
                    if (column == columnCount - 1) spacing else horizontalSpacing,
                    if (row == rowCount - 1) spacing else verticalSpacing
                )
                holder.itemView.layoutParams = layoutParams

                val isVideo: Boolean = videoUrl != null && videoUrl != "null"
                if (isVideo) {
                    val mediaViewHolder = MediaViewHolder(holder.itemView)
                    mediaViewHolder.bindVideo(videoUrl)
                } else {
                    val imageUrl = images[position]
                    if (images.size == 1) {
                        val mediaViewHolder = MediaViewHolder(holder.itemView)
                        mediaViewHolder.bindLargeImage(images[0])
                    } else {
                        val mediaViewHolder = MediaViewHolder(holder.itemView)
                        mediaViewHolder.bindImage(imageUrl)
                    }
                }
            }

            inner class MediaViewHolder(itemView: View) : BaseViewHolder(itemView) {
                private val mediaImageView: ImageView = itemView.findViewById(R.id.image_media)
                private val videoView: VideoView = itemView.findViewById(R.id.video_media)
                private val videoFragLay: FrameLayout = itemView.findViewById(R.id.vedio_FragLay)

                // 在MediaViewHolder中添加进度条控件
                private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_video)

                fun bindImage(imageUrl: String) {
                    mediaImageView.visibility = View.VISIBLE
                    videoFragLay.visibility = View.GONE

                    val layoutParams = mediaImageView.layoutParams
                    layoutParams.width =
                        itemView.context.resources.getDimensionPixelSize(R.dimen.media_grid_width) // 设置ImageView的宽度
                    layoutParams.height = layoutParams.width
                    mediaImageView.layoutParams = layoutParams

                    val requestOptions = RequestOptions()
                        .placeholder(R.drawable.image_placeholder)
                        .centerCrop()

                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .apply(requestOptions)
                        .into(mediaImageView)
                    mediaImageView.setOnClickListener {
                        // 点击事件处理逻辑
                        clickImage=imageUrl
                        // 发布ImageSelectEvent事件
                        val imageselectevent :ImageSelectEvent = ImageSelectEvent(selectedPost,imageUrl)
                        EventBus.getDefault().post(imageselectevent)
                        val intent = Intent(itemView.context, ImagePreviewActivity::class.java)
                        itemView.context.startActivity(intent)
                    }
                }

                fun bindVideo(videoUrl: String?) {
                    mediaImageView.visibility = View.GONE
                    videoFragLay.visibility = View.VISIBLE

                    var isPlaying = false

                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels

                    Log.d("VideoDebug", "Video URL: $videoUrl")
                    videoView.setVideoPath(videoUrl)

                    val layoutParams = videoView.layoutParams
                    layoutParams.width = screenWidth
                    layoutParams.height = layoutParams.width
                    videoView.layoutParams = layoutParams


                    videoView.setOnPreparedListener { mediaPlayer ->
                        Log.d("VideoDebug", "Video prepared, starting playback")
                        mediaPlayer.isLooping = true
                        mediaPlayer.setVolume(0f, 0f)
                        mediaPlayer.start()
                        mediaPlayer.pause() //暂停视频播放
                        // 设置进度条的最大值为视频的总时长
                        progressBar.max = mediaPlayer.duration
                        // 开始更新进度条的进度
                        startUpdatingProgressBar()

                    }
                    videoView.setOnCompletionListener { mediaPlayer ->
                        Log.d("VideoDebug", "Video playback completed")
                        mediaPlayer.seekTo(0)
                        mediaPlayer.start()
                        isPlaying = true
                    }
                    videoView.setOnErrorListener { mp, what, extra ->
                        Log.e("VideoError", "Error occurred while loading video: $what, $extra")
                        false
                    }
                    videoView.setOnClickListener {
                        if (isPlaying) {
                            videoView.pause()
                            isPlaying = false
                        } else {
                            videoView.start()
                            isPlaying = true
                        }
                    }
                    videoView.setOnInfoListener { mp, what, extra ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            // 视频开始渲染
                            isPlaying = true
                        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                            // 视频开始缓冲
                            isPlaying = false
                        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                            // 视频缓冲结束
                            isPlaying = true
                        }
                        false
                    }
                }

                fun bindLargeImage(imageUrl: String) {
                    mediaImageView.visibility = View.VISIBLE
                    videoFragLay.visibility = View.GONE

                    val layoutParams = mediaImageView.layoutParams
                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val scaleFactor = 0.9f // 设置为屏幕宽度的80%
                    val width = (screenWidth * scaleFactor).toInt()

                    val requestOptions = RequestOptions()
                        .placeholder(R.drawable.image_placeholder)
                        .fitCenter()

                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .apply(requestOptions)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                val width = resource?.intrinsicWidth ?: 0
                                val height = resource?.intrinsicHeight ?: 0
                                val isHorizontal = width > height
                                if (isHorizontal) {
                                    // 横图样式screenWidth
                                    layoutParams.width = (screenWidth * scaleFactor).toInt()
                                    layoutParams.height =
                                        (screenWidth / width.toFloat() * height).toInt()
                                } else {
                                    // 竖图样式
                                    layoutParams.width = screenWidth
                                    layoutParams.height =
                                        (layoutParams.width / width.toFloat() * height).toInt()
                                }
                                mediaImageView.layoutParams = layoutParams
                                return false
                            }
                        })
                        .into(mediaImageView)
                    mediaImageView.setOnClickListener {
                        // 点击事件处理逻辑
                        clickImage=imageUrl

                        // 发布ImageSelectEvent事件
                        val imageselectevent :ImageSelectEvent = ImageSelectEvent(selectedPost,imageUrl)
                        EventBus.getDefault().postSticky(imageselectevent)
                        val intent = Intent(itemView.context, ImagePreviewActivity::class.java)
                        itemView.context.startActivity(intent)
                    }
                }

                private fun startUpdatingProgressBar() {
                    // 使用Handler和Runnable来定时更新进度条的进度
                    val handler = Handler(Looper.getMainLooper())
                    val runnable = object : Runnable {
                        override fun run() {
                            // 获取视频当前播放的时间
                            val currentPosition = videoView.currentPosition
                            // 更新进度条的进度
                            progressBar.progress = currentPosition
                            // 每隔一段时间（例如500毫秒）更新一次进度条的进度
                            handler.postDelayed(this, 500)
                        }
                    }
                    // 开始更新进度条的进度
                    handler.postDelayed(runnable, 0)
                }
            }

        }

        fun removeItem(position: Int) {
            if (position in 0 until itemCount) {
                posts.removeAt(position)
                notifyItemRemoved(position)
            }
        }
        private fun likePost(post: Post) {
            // 发送点赞请求
            val requestData = mapOf("id" to post.id)
            val requestJson = Gson().toJson(requestData)
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), requestJson)

            val request = Request.Builder()
                .url("https://hotfix-service-prod.g.mi.com/weibo/like/up")
                .addHeader("Authorization", "Bearer " + token.toString())
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 请求失败处理
                    Log.e("LikeRequest", "点赞请求失败: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    // 请求成功处理
                    val responseData = response.body?.string()
                    val responseJson = JSONObject(responseData)
                    val code = responseJson.getInt("code")
                    when (code) {
                        200 -> {
                            // 更新点赞状态
                            post.likeFlag = true

                            // 发布PostLikeChangeEvent事件
                            EventBus.getDefault().post(PostLikeChangeEvent(post))
                            // 更新UI
                            Handler(Looper.getMainLooper()).post {
                                // 创建点赞动画
                                val likeAnimator = ObjectAnimator.ofPropertyValuesHolder(
                                    likeButton,
                                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.2f, 1.0f),
                                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.2f, 1.0f)
                                )

                                // 设置动画的持续时间为1000毫秒
                                likeAnimator.duration = 1000

                                // 设置动画的插值器为AccelerateDecelerateInterpolator
                                likeAnimator.interpolator = AccelerateDecelerateInterpolator()

                                // 启动动画
                                likeAnimator.start()
                                likeButton.setImageResource(if (post.likeFlag) R.drawable.ic_like else R.drawable.ic_like_filled)
                            }
                        }
                        400 -> {
                            unlikePost(post)
                        }
                        else -> {
                            Log.e("LikeRequest", "点赞请求失败: ${responseJson.getString("msg")}")
                        }
                    }
                }
            })
        }
        private fun unlikePost(post: Post) {
            // 发送取消点赞请求
            val requestData = mapOf("id" to post.id)
            val requestJson = Gson().toJson(requestData)
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), requestJson)

            val request = Request.Builder()
                .url("https://hotfix-service-prod.g.mi.com/weibo/like/down")
                .addHeader("Authorization", "Bearer " + token.toString())
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 请求失败处理
                    Log.e("UnlikeRequest", "取消点赞请求失败: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    // 请求成功处理
                    val responseData = response.body?.string()
                    val responseJson = JSONObject(responseData)
                    val code = responseJson.getInt("code")
                    when (code) {
                        200 -> {
                            // 更新点赞状态
                            post.likeFlag = false

                            // 更新UI
                            Handler(Looper.getMainLooper()).post {
                                // 创建取消点赞动画
                                val unlikeAnimator = ObjectAnimator.ofPropertyValuesHolder(
                                    likeButton,
                                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 0.8f, 1.0f),
                                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 0.8f, 1.0f)
                                )

                                // 设置动画的持续时间为1000毫秒
                                unlikeAnimator.duration = 1000

                                // 设置动画的插值器为AccelerateDecelerateInterpolator
                                unlikeAnimator.interpolator = AccelerateDecelerateInterpolator()

                                // 启动动画
                                unlikeAnimator.start()
                                likeButton.setImageResource(if (post.likeFlag) R.drawable.ic_like else R.drawable.ic_like_filled)
                            }
                            // 发布PostLikeChangeEvent事件
                            EventBus.getDefault().post(PostLikeChangeEvent(post))
                        }
                        400 -> {
                            likePost(post)
                        }
                        else -> {
                            Log.e("UnlikeRequest", "取消点赞请求失败: ${responseJson.getString("msg")}")
                        }
                    }
                }
            })
        }

    }

    private fun istokenuseful() {
        val loginManager = context?.let { LoginManager(it) }
        token = loginManager?.getToken()

        if (token != null) {
            // 检查 token 是否过期
            if (loginManager != null) {
                if (loginManager.isTokenExpired()) {
                    // token 过期，删除本地 token
                    loginManager.clearToken()
                    // TODO: 在这里处理 token 过期的情况
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
        }
    }

}