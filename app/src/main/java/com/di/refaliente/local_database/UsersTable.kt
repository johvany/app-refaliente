package com.di.refaliente.local_database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteOpenHelper
import com.di.refaliente.shared.User

class UsersTable {
    companion object {
        private const val TABLE = "users"

        private val columns = object {
            val idLocal = "id_local"
            val sub = "sub"
            val email = "email"
            val name = "name"
            val surname = "surname"
            val roleUser = "role_user"
            val password = "password"
            val token = "token"
        }

        fun create(db: SQLiteOpenHelper, user: User) {
            db.writableDatabase.insert(
                TABLE,
                null,
                ContentValues().apply {
                    put(columns.idLocal, LocalIDsTable.getLocalId(db, TABLE))
                    put(columns.sub, user.sub)
                    put(columns.email, user.email)
                    put(columns.name, user.name)
                    put(columns.surname, user.surname)
                    put(columns.roleUser, user.roleUser)
                    put(columns.password, user.password)
                    put(columns.token, user.token)
                }
            )
        }

        @SuppressLint("Range")
        fun find(db: SQLiteOpenHelper, idLocal: Int): User? {
            var user: User? = null

            db.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE WHERE ${columns.idLocal} = $idLocal",
                null
            ).use { cursor ->
                if (cursor.moveToNext()) {
                    user = User(
                        cursor.getInt(cursor.getColumnIndex(columns.idLocal)),
                        cursor.getInt(cursor.getColumnIndex(columns.sub)),
                        cursor.getString(cursor.getColumnIndex(columns.email)),
                        cursor.getString(cursor.getColumnIndex(columns.name)),
                        cursor.getString(cursor.getColumnIndex(columns.surname)),
                        cursor.getString(cursor.getColumnIndex(columns.roleUser)),
                        cursor.getString(cursor.getColumnIndex(columns.password)),
                        cursor.getString(cursor.getColumnIndex(columns.token))
                    )
                }
            }

            return user
        }

        fun update(db: SQLiteOpenHelper, user: User) {
            db.writableDatabase.update(
                TABLE,
                ContentValues().apply {
                    put(columns.sub, user.sub)
                    put(columns.email, user.email)
                    put(columns.name, user.name)
                    put(columns.surname, user.surname)
                    put(columns.roleUser, user.roleUser)
                    put(columns.password, user.password)
                    put(columns.token, user.token)
                },
                "${columns.idLocal} = ${user.idLocal}",
                null
            )
        }
    }
}