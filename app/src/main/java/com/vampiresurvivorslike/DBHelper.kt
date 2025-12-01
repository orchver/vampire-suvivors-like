package com.vampiresurvivorslike

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import kotlin.apply

class DBHelper(context: Context) : SQLiteOpenHelper(context, "UserDB.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE users (id TEXT PRIMARY KEY, password TEXT)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }
    fun insertUser(id: String, password: String): Boolean {
        val values = ContentValues().apply {
            put("id", id)
            put("password", password)
        }
        val db = this.writableDatabase
        val result = db.insert("users", null, values)
        db.close()
        return result != -1L
    }
    fun login(id: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM users WHERE id=? AND password=?",
            arrayOf(id, password)
        )
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }
}
