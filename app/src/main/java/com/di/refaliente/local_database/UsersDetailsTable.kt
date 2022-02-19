package com.di.refaliente.local_database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import com.di.refaliente.shared.UserDetail

class UsersDetailsTable {
    companion object {
        private const val TABLE = "users_details"

        private val columns = object {
            val idLocal = "id_local"
            val keySub = "key_sub"
            val name = "name"
            val surname = "surname"
            val email = "email"
            val description = "description"
            val keyTypeUser = "key_type_user"
            val keyBusinessType = "key_business_type"
            val telephone = "telephone"
            val profileImage = "profile_image"
            val sessionType = "session_type"
            val rfc = "rfc"
            val socialReason = "social_reason"
            val enterpriseName = "enterprise_name"
        }

        fun create(db: SQLiteOpenHelper, userDetail: UserDetail) {
            db.writableDatabase.insert(
                TABLE,
                null,
                ContentValues().apply {
                    put(columns.idLocal, LocalIDsTable.getLocalId(db, TABLE))
                    put(columns.keySub, userDetail.keySub)
                    put(columns.name, userDetail.name)
                    put(columns.surname, userDetail.surname)
                    put(columns.email, userDetail.email)
                    if (userDetail.description == null) { putNull(columns.description) } else { put(columns.description, userDetail.description) }
                    put(columns.keyTypeUser, userDetail.keyTypeUser)
                    put(columns.keyBusinessType, userDetail.keyBusinessType)
                    if (userDetail.telephone == null) { putNull(columns.telephone) } else { put(columns.telephone, userDetail.telephone) }
                    if (userDetail.profileImage == null) { putNull(columns.profileImage) } else { put(columns.profileImage, userDetail.profileImage) }
                    put(columns.sessionType, userDetail.sessionType)
                    if (userDetail.rfc == null) { putNull(columns.rfc) } else { put(columns.rfc, userDetail.rfc) }
                    if (userDetail.socialReason == null) { putNull(columns.socialReason) } else { put(columns.socialReason, userDetail.socialReason) }
                    if (userDetail.enterpriseName == null) { putNull(columns.enterpriseName) } else { put(columns.enterpriseName, userDetail.enterpriseName) }
                }
            )
        }

        @SuppressLint("Range")
        fun find(db: SQLiteOpenHelper, idLocal: Int): UserDetail? {
            var userDetail: UserDetail? = null

            db.readableDatabase.rawQuery(
                "SELECT * FROM $TABLE WHERE ${columns.idLocal} = $idLocal",
                null
            ).use { cursor ->
                if (cursor.moveToNext()) {
                    userDetail = UserDetail(
                        cursor.getInt(cursor.getColumnIndex(columns.idLocal)),
                        cursor.getInt(cursor.getColumnIndex(columns.keySub)),
                        cursor.getString(cursor.getColumnIndex(columns.name)),
                        cursor.getString(cursor.getColumnIndex(columns.surname)),
                        cursor.getString(cursor.getColumnIndex(columns.email)),
                        cursor.getStringOrNull(cursor.getColumnIndex(columns.description)),
                        cursor.getInt(cursor.getColumnIndex(columns.keyTypeUser)),
                        cursor.getInt(cursor.getColumnIndex(columns.keyBusinessType)),
                        cursor.getStringOrNull(cursor.getColumnIndex(columns.telephone)),
                        cursor.getStringOrNull(cursor.getColumnIndex(columns.profileImage)),
                        cursor.getString(cursor.getColumnIndex(columns.sessionType)),
                        cursor.getStringOrNull(cursor.getColumnIndex(columns.rfc)),
                        cursor.getStringOrNull(cursor.getColumnIndex(columns.socialReason)),
                        cursor.getStringOrNull(cursor.getColumnIndex(columns.enterpriseName))
                    )
                }
            }

            return userDetail
        }

        fun update(db: SQLiteOpenHelper, userDetail: UserDetail) {
            db.writableDatabase.update(
                TABLE,
                ContentValues().apply {
                    put(columns.keySub, userDetail.keySub)
                    put(columns.name, userDetail.name)
                    put(columns.surname, userDetail.surname)
                    put(columns.email, userDetail.email)
                    if (userDetail.description == null) { putNull(columns.description) } else { put(columns.description, userDetail.description) }
                    put(columns.keyTypeUser, userDetail.keyTypeUser)
                    put(columns.keyBusinessType, userDetail.keyBusinessType)
                    if (userDetail.telephone == null) { putNull(columns.telephone) } else { put(columns.telephone, userDetail.telephone) }
                    if (userDetail.profileImage == null) { putNull(columns.profileImage) } else { put(columns.profileImage, userDetail.profileImage) }
                    put(columns.sessionType, userDetail.sessionType)
                    if (userDetail.rfc == null) { putNull(columns.rfc) } else { put(columns.rfc, userDetail.rfc) }
                    if (userDetail.socialReason == null) { putNull(columns.socialReason) } else { put(columns.socialReason, userDetail.socialReason) }
                    if (userDetail.enterpriseName == null) { putNull(columns.enterpriseName) } else { put(columns.enterpriseName, userDetail.enterpriseName) }
                },
                "${columns.idLocal} = ${userDetail.idLocal}",
                null
            )
        }
    }
}