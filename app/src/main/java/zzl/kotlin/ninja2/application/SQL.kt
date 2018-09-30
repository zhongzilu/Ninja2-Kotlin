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
}

/**
 * 历史访问记录数据库表对象
 */
object RecordTable {
    val NAME = "Record"
    val TITLE = "title"
    val URL = "url"
    val TIME = "time"
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
                PinTable.URL to TEXT)

        //创建历史记录表
        db?.createTable(RecordTable.NAME, true,
                RecordTable.URL to TEXT + PRIMARY_KEY,
                RecordTable.TITLE to TEXT,
                RecordTable.TIME to TEXT)
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
     */
    fun savePin(pin: Pin) {
        SQL.instance.use {
            insert(PinTable.NAME,
                    PinTable.TITLE to pin.title,
                    PinTable.URL to pin.url,
                    PinTable.ID to pin._id)
        }
    }

    /**
     * 保存访问的网站作为Pin记录，该记录将出现在首页Pin列表
     */
    fun savePin(title: String, url: String){
        SQL.instance.use {
            insert(PinTable.NAME,
                    PinTable.TITLE to title,
                    PinTable.URL to url,
                    PinTable.ID to 0)
        }
    }

    /**
     * 获取所有的Pin记录
     */
    fun findAllPins(): List<Pin> {
        return SQL.instance.use {
            select(PinTable.NAME).parseList(classParser<Pin>())
        }
    }

    //==========Record相关操作============

    /**
     * 保存访问的网站作为历史纪录
     */
    fun saveRecord(title: String, url: String) {
        SQL.instance.use {
            insert(RecordTable.NAME,
                    RecordTable.TITLE to title,
                    RecordTable.URL to url,
                    RecordTable.TIME to System.currentTimeMillis())
        }
    }

    /**
     * 获取所有历史记录
     */
    fun findAllRecords(): List<Record> {
        return SQL.instance.use {
            select(PinTable.NAME).parseList(classParser<Record>())
        }
    }

    /**
     * 搜索匹配关键字的历史记录，比如根据url或名称进行搜索
     */
    fun searchRecord(key: String): List<Record> {
        return SQL.instance.use {
            select(RecordTable.NAME).whereArgs("title LIKE '%{key}%' OR url LIKE '%{key}%' ORDER BY time DESC",
                    "key" to key)
                    .parseList(classParser<Record>())
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
     */
    fun clearOldRecord(time: Long = 1296000000){
        SQL.instance.use {
            delete(RecordTable.NAME, "time < {time}",
                    "time" to (System.currentTimeMillis() - time))
        }
    }
}