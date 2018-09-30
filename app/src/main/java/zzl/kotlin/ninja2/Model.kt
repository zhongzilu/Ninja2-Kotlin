package zzl.kotlin.ninja2

/**
 * 存放所有实体数据类
 * Created by zhongzilu on 18-9-19.
 */


/**
 * 主页Pin
 */
data class Pin(
        val _id: Int = 0,
        val title: String,
        val url: String
)

/**
 * 访问历史记录
 */
data class Record(
        val title: String,
        val url: String,
        val time: Long = 0L
)