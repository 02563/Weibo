package com.example.weibo_lvruizhong.ui.home.Recycler

data class Post(
    val id: Int,
    val userId: Int,
    val username: String,
    val phone: String,
    val avatar: String,
    val title: String,
    val videourl: String?,
    val poster: String?,
    val images: MutableList<String>,
    val likeCount: Int,
    var likeFlag: Boolean,
    val createTime: String
)
