package zzl.kotlin.ninja2

/**
 * 存放所有实体数据类
 * Created by zhongzilu on 18-9-19.
 */


/**
 * 主页Pin
 */
data class Pin(
        var _id: Int = 0,
        var title: String,
        val url: String,
        val visit: Int = 0
)

/**
 * 访问历史记录
 */
data class Record(
        val title: String,
        val url: String,
        val time: String,
        val visit: Int = 0
)