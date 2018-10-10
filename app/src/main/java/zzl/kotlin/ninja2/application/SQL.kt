package zzl.kotlin.ninja2.application

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*
import zzl.kotlin.ninja2.App
import zzl.kotlin.ninja2.Pin
import zzl.kotlin.ninja2.Record

/**
 * SQLite操作相关的类
 * Created by zhongzilu on 2018-09-30
 */

/**
 * Pin数据库表对象
 */
object PinTable {
    val NAME = "Pin"
    val ID = "_id"
    val TITLE = "title"
    val URL = "url"
    val VISIT = "visit"
}

/**
 * 历史访问记录数据库表对象
 */
object RecordTable {
    val NAME = "Record"
    val TITLE = "title"
    val URL = "url"
    val TIME = "time"
    val VISIT = "visit"
}

/**
 * 数据库操作类
 */
class SQL(ctx: Context = App.instance) : ManagedSQLiteOpenHelper(ctx, SQL.DB_NAME, null, SQL.DB_VERSION) {

    companion object {
        const val DB_NAME = "ninja2.db"
        const val DB_VERSION = 1
        val instance: SQL by lazy { SQL() }
    }

    override fun onCreate(db: SQLiteDatabase?) {

        //创建Pin数据库表
        db?.createTable(PinTable.NAME, true,
                PinTable.ID to INTEGER + PRIMARY_KEY,
                PinTable.TITLE to TEXT,
                PinTable.URL to TEXT,
                PinTable.VISIT to INTEGER)

        //创建历史记录表
        db?.createTable(RecordTable.NAME, true,
                RecordTable.TITLE to TEXT,
                RecordTable.URL to TEXT + PRIMARY_KEY,
                RecordTable.TIME to TEXT,
                RecordTable.VISIT to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.dropTable(PinTable.NAME, true)
        db?.dropTable(RecordTable.NAME, true)
        onCreate(db)
    }
}

//abstract class SQLModel {
//
//    val NAME = this.javaClass.simpleName.toUpperCase()
//
//    fun save(){
//        val values = ContentValues()
//        this.javaClass.declaredFields.forEach {
//            values.put(it.name, it[this].toString())
//        }
//
//        SQL.instance.use {
//            insert(NAME, null, values)
//        }
//    }
//
//    inline fun <reified T: Any> findAll(){
//        SQL.instance.use {
//            select(NAME).parseList(classParser<T>())
//        }
//    }
//
//}

/**
 * 数据库操作辅助类
 */
object SQLHelper {

    /**
     * 保存Pin对象
     *
     * @param pin 待保存的数据对象
     */
    fun savePin(pin: Pin) {
        SQL.instance.use {
            insert(PinTable.NAME,
                    PinTable.TITLE to pin.title,
                    PinTable.URL to pin.url,
                    PinTable.VISIT to pin.visit)
        }
    }

    /**
     * 保存访问的网站作为Pin记录，该记录将出现在首页Pin列表
     *
     * @param title 网站标题
     * @param url 网站网址
     */
    fun savePin(title: String, url: String) {
        SQL.instance.use {
            insert(PinTable.NAME,
                    PinTable.TITLE to title,
                    PinTable.URL to url,
                    PinTable.VISIT to 0)
        }
    }

    /**
     * 获取所有的Pin记录
     *
     * @return List<Pin>
     */
    fun findAllPins(): List<Pin> {
        return SQL.instance.use {
            select(PinTable.NAME).parseList(classParser())
        }
    }

    /**
     * 根据Pin记录ID来更新其他数据字段
     *
     * @param pin 待更新的数据对象
     */
    fun updatePinById(pin: Pin) {
        SQL.instance.use {
            update(PinTable.NAME,
                    PinTable.TITLE to pin.title,
                    PinTable.URL to pin.url)
                    .whereArgs("_id = {id}", "id" to pin._id)
                    .exec()
        }
    }

    /**
     * 删除Pin记录ID对应的记录
     *
     * @param pin 待删除的数据对象
     */
    fun deletePin(pin: Pin){
        SQL.instance.use {
            delete(PinTable.NAME, "_id = {id}", "id" to pin._id)
        }
    }


    //==========Record相关操作============

    /**
     * 保存访问的网站作为历史纪录，
     * 如果不存在相同url的记录，则新增记录后退出，
     * 如果存在相同url的记录，则更新该条记录后退出
     *
     * @param title 访问网站的标题
     * @param url 访问网站的网址
     */
    fun saveOrUpdateRecord(title: String, url: String) {
        SQL.instance.use {

            val records = select(RecordTable.NAME).parseList(classParser<Record>())
            records.forEach {
                if (it.url == url) {
                    //update record data
                    update(RecordTable.NAME,
                            RecordTable.TITLE to title,
                            RecordTable.URL to url,
                            RecordTable.TIME to System.currentTimeMillis(),
                            RecordTable.VISIT to it.visit.inc())
                            .whereArgs("url = {url}", "url" to it.url)
                            .exec()

                    return@use
                }
            }

            insert(RecordTable.NAME,
                    RecordTable.TITLE to title,
                    RecordTable.URL to url,
                    RecordTable.TIME to System.currentTimeMillis(),
                    RecordTable.VISIT to 0)
        }
    }

    /**
     * 获取所有历史记录
     *
     * @return List<Record>
     */
    fun findAllRecords(): List<Record> {
        return SQL.instance.use {
            select(PinTable.NAME).parseList(classParser())
        }
    }

    /**
     * 搜索匹配关键字的历史记录，比如根据url或名称进行搜索，
     * 结果以{@link RecordTable.VISIT}字段倒序排列
     *
     * @param key 搜索关键字，该关键字将用于
     */
    fun searchRecord(key: String): List<Record> {
        return SQL.instance.use {
            select(RecordTable.NAME).whereArgs("title LIKE {key} OR url LIKE {key} ORDER BY visit DESC",
                    "key" to "%$key%")
                    .parseList(classParser())
        }
    }

    /**
     * 清除所有历史纪录
     */
    fun clearAllRecord() {
        SQL.instance.use {
            delete(RecordTable.NAME)
        }
    }

    /**
     * 删除N天前的历史记录，默认为15天前
     *
     * @param time 时间戳，该方法将删除数据库中创建时间早于该时间戳的记录
     */
    fun clearOldRecord(time: Long = 1296000000) {
        SQL.instance.use {
            delete(RecordTable.NAME, "time < {time}",
                    "time" to (System.currentTimeMillis() - time))
        }
    }
}