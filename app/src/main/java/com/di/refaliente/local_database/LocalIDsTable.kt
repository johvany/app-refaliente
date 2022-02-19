package com.di.refaliente.local_database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteOpenHelper

class LocalIDsTable {
    companion object {
        private const val TABLE = "local_ids"

        private val columns = object {
            val tableName = "table_name"
            val availableID = "available_id"
        }

        @SuppressLint("Range")
        fun getLocalId(db: SQLiteOpenHelper, tableName: String): Int {
            val id: Int

            db.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE WHERE ${columns.tableName} LIKE '$tableName'",
                null
            ).use { cursor ->
                cursor.moveToNext()
                id = cursor.getInt(cursor.getColumnIndex(columns.availableID))
            }

            db.writableDatabase.update(
                TABLE,
                ContentValues().apply { put(columns.availableID, id + 1) },
                "${columns.tableName} LIKE '$tableName'",
                null
            )

            return id
        }
    }
}