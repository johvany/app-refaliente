package com.di.refaliente.local_database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Database(context: Context) : SQLiteOpenHelper(context, "main_database.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        val values = ContentValues()

        db?.execSQL("CREATE TABLE IF NOT EXISTS local_ids(" +
                "table_name text," +
                "available_id integer)")

        db?.execSQL("CREATE TABLE IF NOT EXISTS users(" +
                "id_local integer," +
                "sub integer," +
                "email text," +
                "name text," +
                "surname text," +
                "role_user text," +
                "password text," +
                "token text)")

        values.clear()
        values.put("table_name", "users")
        values.put("available_id", 1)
        db?.insert("local_ids", null, values)

        db?.execSQL("CREATE TABLE IF NOT EXISTS users_details(" +
                "id_local integer," +
                "key_sub integer," +
                "name text," +
                "surname text," +
                "email text," +
                "description text," +
                "key_type_user integer," +
                "key_business_type integer," +
                "telephone text," +
                "profile_image text," +
                "session_type text," +
                "rfc text," +
                "social_reason text," +
                "enterprise_name text)")

        values.clear()
        values.put("table_name", "users_details")
        values.put("available_id", 1)
        db?.insert("local_ids", null, values)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // ...
    }
}