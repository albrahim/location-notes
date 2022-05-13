package com.example.locationnotes

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(
    val context: Context,
    ) : SQLiteOpenHelper(context, dbName, null, dbVersion) {
    companion object {
        val dbName = "LocationTrackerDB"
        val dbVersion = 5
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE places (
            uid INTEGER PRIMARY KEY AUTOINCREMENT,
            lat REAL NOT NULL,
            lon REAL NOT NULL,
            time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
            description TEXT NOT NULL,
            hue INTEGER NOT NULL
            )
        """.trimIndent()
        db!!.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let { db ->
            val dropTableQuery = "DROP TABLE IF EXISTS places"
            db.execSQL(dropTableQuery)
            onCreate(db)
        }
    }
}