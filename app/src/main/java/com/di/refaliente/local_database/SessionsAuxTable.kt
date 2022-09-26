package com.di.refaliente.local_database

import android.content.ContentValues
import android.database.sqlite.SQLiteOpenHelper
import com.di.refaliente.shared.SessionAux

class SessionsAuxTable {
    companion object {
        private const val TABLE = "sessions_aux"
        private const val COL_ID_USER = "id_user"
        private const val COL_TOKEN_ID = "token_id"

        fun insert(db: SQLiteOpenHelper, item: SessionAux) {
            db.writableDatabase.insert(TABLE, null, ContentValues().apply {
                put(COL_ID_USER, item.idUser)
                put(COL_TOKEN_ID, item.tokenId)
            })
        }

        fun find(db: SQLiteOpenHelper, idUser: Int): SessionAux? {
            val item: SessionAux?

            db.readableDatabase.rawQuery("SELECT * FROM $TABLE WHERE $COL_ID_USER = $idUser", null).use { cursor ->
                item = if (cursor.moveToNext()) {
                    SessionAux(
                        cursor.getInt(cursor.getColumnIndex(COL_ID_USER)),
                        cursor.getInt(cursor.getColumnIndex(COL_TOKEN_ID))
                    )
                } else {
                    null
                }
            }

            return item
        }

        fun update(db: SQLiteOpenHelper, item: SessionAux) {
            db.writableDatabase.update(
                TABLE,
                ContentValues().apply { put(COL_TOKEN_ID, item.tokenId) },
                "$COL_ID_USER = ${item.idUser}",
                null
            )
        }
    }
}