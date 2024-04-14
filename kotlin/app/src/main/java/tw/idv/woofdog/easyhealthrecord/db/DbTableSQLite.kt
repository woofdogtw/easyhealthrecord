package tw.idv.woofdog.easyhealthrecord.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import java.util.Vector

/**
 * The SQLite implementation of DbTableBase.
 *
 * This class implements complete database operations for this program, and uses SQLite for updating
 * and querying data.
 */
class DbTableSQLite private constructor() : DbTableBase {
    enum class AvailableType {
        /** Invalid database format. */
        INVALID,

        /** Old formats. */
        READ_ONLY,

        /** The current database format. */
        READ_WRITE
    }

    constructor(dbVer: Int = DB_TOP_VERSION) : this() {
        isReadOnly = dbVer < DB_TOP_VERSION
        dbVersion = dbVer
    }

    companion object {
        private const val DB_NAME = "Easy Health Record"
        private const val DB_VERSION_V1 = 1
        private const val DB_VERSION_V2 = 2
        private const val DB_TOP_VERSION = DB_VERSION_V2

        private const val COL_IDX_NAME = 0
        private const val COL_IDX_VERSION = 1
        private const val COL_IDX_DESCRIPTION = 2
        private const val COL_IDX_LAST_MODIFY = 3

        private const val COL_IDX_BW_ID = 0
        private const val COL_IDX_BW_DATE = 1
        private const val COL_IDX_BW_WEIGHT = 2
        private const val COL_IDX_BW_FAT = 3
        private const val COL_IDX_BW_INT_FAT = 4
        private const val COL_IDX_BW_BMI = 5
        private const val COL_IDX_BW_WC = 6
        private const val COL_IDX_BW_BONE = 7
        private const val COL_IDX_BW_MUSCLE = 8
        private const val COL_IDX_BW_WATER = 9
        private const val COL_IDX_BW_METABOLIC = 10
        private const val COL_IDX_BW_AGE = 11
        private const val COL_IDX_BW_COMMENT = 12

        private const val COL_IDX_BP_ID = 0
        private const val COL_IDX_BP_DATE = 1
        private const val COL_IDX_BP_SYSTOLIC = 2
        private const val COL_IDX_BP_DIASTOLIC = 3
        private const val COL_IDX_BP_PULSE = 4
        private const val COL_IDX_BP_COMMENT = 5

        private const val COL_IDX_BG_ID = 0
        private const val COL_IDX_BG_DATE = 1
        private const val COL_IDX_BG_GLUCOSE = 2
        private const val COL_IDX_BG_MEAL = 3
        private const val COL_IDX_BG_COMMENT = 4

        /**
         * To check the database format.
         */
        fun isAvailableDatabase(dbPath: String, dbVer: Int = DB_TOP_VERSION): AvailableType {
            val logTag = "DbTableSQLite.isAvailableDatabase()"

            val db: SQLiteDatabase
            try {
                db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "")
                return AvailableType.INVALID
            }

            val validType = innerIsAvailableDatabase(db, dbVer)
            try {
                db.close()
            } catch (e: Exception) {
                Log.e(logTag, e.message ?: "close error")
            }
            return validType
        }

        private fun innerIsAvailableDatabase(db: SQLiteDatabase, dbVer: Int): AvailableType {
            val logTag = "DbTableSQLite.innerIsAvailableDatabase()"

            if (!db.isOpen) {
                return AvailableType.INVALID
            }

            var availableType = AvailableType.INVALID
            var cursor: Cursor? = null
            do {
                try {
                    cursor = db.rawQuery("SELECT * FROM db_info", arrayOf())
                    if (cursor == null || !cursor.moveToNext()) {
                        break
                    }
                    val name = cursor.getString(COL_IDX_NAME)
                    val version = cursor.getInt(COL_IDX_VERSION)
                    availableType = if (
                        name != DB_NAME || dbVer < DB_VERSION_V1 || version > dbVer
                    ) {
                        break
                    } else if (version != dbVer) {
                        AvailableType.READ_ONLY
                    } else {
                        AvailableType.READ_WRITE
                    }
                } catch (e: SQLiteException) {
                    Log.e(logTag, e.message ?: "query error")
                }
            } while (false)

            cursor?.close()
            return availableType
        }
    }

    override fun getDescription(): String? {
        val logTag = "DbTableSQLite.getDescription()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var description: String? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery("SELECT descript FROM db_info", arrayOf()) ?: break
                description = if (cursor.moveToNext()) cursor.getString(0) else null
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return description
    }

    override fun getLastModified(): Long? {
        val logTag = "DbTableSQLite.getLastModified()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var lastModified: Long? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT last_modify FROM db_info", arrayOf()) ?: break
                lastModified = if (cursor.moveToNext()) cursor.getLong(0) else null
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return lastModified
    }

    override fun setFileName(name: String): Boolean {
        val logTag = "DbTableSQLite.setFileName()"

        dbFileName = name

        if (dbConn != null && dbConn?.isOpen!!) {
            dbConn?.close()
        }
        dbConn = null
        if (name == "") {
            return true
        }

        try {
            dbConn = SQLiteDatabase.openOrCreateDatabase(dbFileName, null)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "create error")
        }
        if (!createDatabase()) {
            return false
        }

        isReadOnly = true
        var success = false
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT version FROM db_info", arrayOf()) ?: break
                dbVersion = if (cursor.moveToNext()) cursor.getInt(0) else break
                isReadOnly = !updateDatabase(dbVersion)
                success = true
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query version error")
            }
        } while (false)

        cursor?.close()
        return success
    }

    override fun setDescription(description: String): Boolean {
        val logTag = "DbTableSQLite.setDescription()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("UPDATE db_info SET descript=?", arrayOf(description))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_info error")
            return false
        }
        setLastModified()
        return true
    }

    override fun setLastModified(lastTime: Long): Boolean {
        val logTag = "DbTableSQLite.setLastModified()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("UPDATE db_info SET last_modify=?", arrayOf("$lastTime"))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "update error")
            return false
        }
        return true
    }

    override fun getBodyWeightNumber(): Int? {
        val logTag = "DbTableSQLite.getBodyWeightNumber()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var count: Int? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT COUNT(*) FROM db_body_weight", arrayOf()) ?: break
                count = if (cursor.moveToNext()) cursor.getInt(0) else break
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return count
    }

    override fun getBodyWeights(): Vector<DbBodyWeight>? {
        val logTag = "DbTableSQLite.getBodyWeights()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var bwList: Vector<DbBodyWeight>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT * FROM db_body_weight ORDER BY date,id", arrayOf())
                        ?: break
                bwList = Vector<DbBodyWeight>()
                while (cursor.moveToNext()) {
                    val b = DbBodyWeight(cursor.getLong(COL_IDX_BW_ID))
                    b.mDate = cursor.getLong(COL_IDX_BW_DATE)
                    b.mWeight = cursor.getDouble(COL_IDX_BW_WEIGHT)
                    b.mFat = cursor.getDouble(COL_IDX_BW_FAT)
                    b.mIntFat = cursor.getDouble(COL_IDX_BW_INT_FAT)
                    b.mBmi = cursor.getDouble(COL_IDX_BW_BMI)
                    b.mWc = cursor.getDouble(COL_IDX_BW_WC)
                    b.mBone = cursor.getDouble(COL_IDX_BW_BONE)
                    b.mMuscle = cursor.getDouble(COL_IDX_BW_MUSCLE)
                    b.mWater = cursor.getDouble(COL_IDX_BW_WATER)
                    b.mMetabolic = cursor.getInt(COL_IDX_BW_METABOLIC)
                    b.mAge = cursor.getInt(COL_IDX_BW_AGE)
                    b.mComment = cursor.getString(COL_IDX_BW_COMMENT) ?: ""

                    bwList.add(b)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return bwList
    }

    override fun getBodyWeights(from: Long, to: Long): Vector<DbBodyWeight>? {
        val logTag = "DbTableSQLite.getBodyWeights(from, to)"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var bwList: Vector<DbBodyWeight>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery(
                        "SELECT * FROM db_body_weight WHERE date BETWEEN ? AND ? ORDER BY date,id",
                        arrayOf("$from", "$to")
                    )
                        ?: break
                bwList = Vector<DbBodyWeight>()
                while (cursor.moveToNext()) {
                    val b = DbBodyWeight(cursor.getLong(COL_IDX_BW_ID))
                    b.mDate = cursor.getLong(COL_IDX_BW_DATE)
                    b.mWeight = cursor.getDouble(COL_IDX_BW_WEIGHT)
                    b.mFat = cursor.getDouble(COL_IDX_BW_FAT)
                    b.mIntFat = cursor.getDouble(COL_IDX_BW_INT_FAT)
                    b.mBmi = cursor.getDouble(COL_IDX_BW_BMI)
                    b.mWc = cursor.getDouble(COL_IDX_BW_WC)
                    b.mBone = cursor.getDouble(COL_IDX_BW_BONE)
                    b.mMuscle = cursor.getDouble(COL_IDX_BW_MUSCLE)
                    b.mWater = cursor.getDouble(COL_IDX_BW_WATER)
                    b.mMetabolic = cursor.getInt(COL_IDX_BW_METABOLIC)
                    b.mAge = cursor.getInt(COL_IDX_BW_AGE)
                    b.mComment = cursor.getString(COL_IDX_BW_COMMENT) ?: ""

                    bwList.add(b)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return bwList
    }

    override fun getBodyWeight(id: Long): DbBodyWeight? {
        val logTag = "DbTableSQLite.getBodyWeight()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var b: DbBodyWeight? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_BODY_WEIGHT, arrayOf("$id")) ?: break
                if (!cursor.moveToNext()) {
                    break
                }
                b = DbBodyWeight(id)
                b.mDate = cursor.getLong(COL_IDX_BW_DATE)
                b.mWeight = cursor.getDouble(COL_IDX_BW_WEIGHT)
                b.mFat = cursor.getDouble(COL_IDX_BW_FAT)
                b.mIntFat = cursor.getDouble(COL_IDX_BW_INT_FAT)
                b.mBmi = cursor.getDouble(COL_IDX_BW_BMI)
                b.mWc = cursor.getDouble(COL_IDX_BW_WC)
                b.mBone = cursor.getDouble(COL_IDX_BW_BONE)
                b.mMuscle = cursor.getDouble(COL_IDX_BW_MUSCLE)
                b.mWater = cursor.getDouble(COL_IDX_BW_WATER)
                b.mMetabolic = cursor.getInt(COL_IDX_BW_METABOLIC)
                b.mAge = cursor.getInt(COL_IDX_BW_AGE)
                b.mComment = cursor.getString(COL_IDX_BW_COMMENT) ?: ""
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return b
    }

    override fun addBodyWeight(bw: DbBodyWeight): Boolean {
        val logTag = "DbTableSQLite.addBodyWeight()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery(QUERY_BODY_WEIGHT, arrayOf("${bw.mId}")) ?: return false
                if (cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "INSERT INTO db_body_weight " +
                    "(id, date, weight, fat, int_fat, bmi, wc, bone, muscle, water,metabolic, " +
                    "age, comment) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            val values = arrayOf(
                "${bw.mId}",
                "${bw.mDate}",
                "${bw.mWeight}",
                "${bw.mFat}",
                "${bw.mIntFat}",
                "${bw.mBmi}",
                "${bw.mWc}",
                "${bw.mBone}",
                "${bw.mMuscle}",
                "${bw.mWater}",
                "${bw.mMetabolic}",
                "${bw.mAge}",
                bw.mComment
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "insert error")
            return false
        }
        setLastModified()
        return true
    }

    override fun deleteBodyWeight(id: Long): Boolean {
        val logTag = "DbTableSQLite.deleteBodyWeight()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("DELETE FROM db_body_weight WHERE id=?", arrayOf(id))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "delete error")
            return false
        }
        setLastModified()
        return true
    }

    override fun modifyBodyWeight(id: Long, bw: DbBodyWeight): Boolean {
        val logTag = "DbTableSQLite.modifyBodyWeight()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_BODY_WEIGHT, arrayOf("$id")) ?: return false
                if (!cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "UPDATE db_body_weight " +
                    "SET date=?, weight=?, fat=?, int_fat=?, bmi=?, wc=?, bone=?, " +
                    "muscle=?, water=?, metabolic=?, age=?, comment=? " +
                    "WHERE id=?"
            val values = arrayOf(
                "${bw.mDate}",
                "${bw.mWeight}",
                "${bw.mFat}",
                "${bw.mIntFat}",
                "${bw.mBmi}",
                "${bw.mWc}",
                "${bw.mBone}",
                "${bw.mMuscle}",
                "${bw.mWater}",
                "${bw.mMetabolic}",
                "${bw.mAge}",
                bw.mComment,
                "$id"
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "update error")
            return false
        }
        setLastModified()
        return true
    }

    override fun getBloodPressureNumber(): Int? {
        val logTag = "DbTableSQLite.getBloodPressureNumber()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var count: Int? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT COUNT(*) FROM db_blood_pressure", arrayOf()) ?: break
                count = if (cursor.moveToNext()) cursor.getInt(0) else break
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return count
    }

    override fun getBloodPressures(): Vector<DbBloodPressure>? {
        val logTag = "DbTableSQLite.getBloodPressures()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var bpList: Vector<DbBloodPressure>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT * FROM db_blood_pressure ORDER BY date,id", arrayOf())
                        ?: break
                bpList = Vector<DbBloodPressure>()
                while (cursor.moveToNext()) {
                    val b = DbBloodPressure(cursor.getLong(COL_IDX_BP_ID))
                    b.mDate = cursor.getLong(COL_IDX_BP_DATE)
                    b.mSystolic = cursor.getInt(COL_IDX_BP_SYSTOLIC)
                    b.mDiastolic = cursor.getInt(COL_IDX_BP_DIASTOLIC)
                    b.mPulse = cursor.getInt(COL_IDX_BP_PULSE)
                    b.mComment = cursor.getString(COL_IDX_BP_COMMENT) ?: ""

                    bpList.add(b)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return bpList
    }

    override fun getBloodPressures(from: Long, to: Long): Vector<DbBloodPressure>? {
        val logTag = "DbTableSQLite.getBloodPressures(from, to)"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var bpList: Vector<DbBloodPressure>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery(
                        "SELECT * FROM db_blood_pressure WHERE date BETWEEN ? AND ? " +
                                "ORDER BY date,id",
                        arrayOf("$from", "$to")
                    )
                        ?: break
                bpList = Vector<DbBloodPressure>()
                while (cursor.moveToNext()) {
                    val b = DbBloodPressure(cursor.getLong(COL_IDX_BP_ID))
                    b.mDate = cursor.getLong(COL_IDX_BP_DATE)
                    b.mSystolic = cursor.getInt(COL_IDX_BP_SYSTOLIC)
                    b.mDiastolic = cursor.getInt(COL_IDX_BP_DIASTOLIC)
                    b.mPulse = cursor.getInt(COL_IDX_BP_PULSE)
                    b.mComment = cursor.getString(COL_IDX_BP_COMMENT) ?: ""

                    bpList.add(b)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return bpList
    }

    override fun getBloodPressure(id: Long): DbBloodPressure? {
        val logTag = "DbTableSQLite.getBloodPressure()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var b: DbBloodPressure? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_BLOOD_PRESSURE, arrayOf("$id")) ?: break
                if (!cursor.moveToNext()) {
                    break
                }
                b = DbBloodPressure(id)
                b.mDate = cursor.getLong(COL_IDX_BP_DATE)
                b.mSystolic = cursor.getInt(COL_IDX_BP_SYSTOLIC)
                b.mDiastolic = cursor.getInt(COL_IDX_BP_DIASTOLIC)
                b.mPulse = cursor.getInt(COL_IDX_BP_PULSE)
                b.mComment = cursor.getString(COL_IDX_BP_COMMENT) ?: ""
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return b
    }

    override fun addBloodPressure(bp: DbBloodPressure): Boolean {
        val logTag = "DbTableSQLite.addBloodPressure()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery(QUERY_BLOOD_PRESSURE, arrayOf("${bp.mId}")) ?: return false
                if (cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "INSERT INTO db_blood_pressure " +
                    "(id, date, systolic, diastolic, pulse, comment) " +
                    "VALUES (?, ?, ?, ?, ?, ?)"
            val values = arrayOf(
                "${bp.mId}",
                "${bp.mDate}",
                "${bp.mSystolic}",
                "${bp.mDiastolic}",
                "${bp.mPulse}",
                bp.mComment
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "insert error")
            return false
        }
        setLastModified()
        return true
    }

    override fun deleteBloodPressure(id: Long): Boolean {
        val logTag = "DbTableSQLite.deleteBloodPressure()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("DELETE FROM db_blood_pressure WHERE id=?", arrayOf(id))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "delete error")
            return false
        }
        setLastModified()
        return true
    }

    override fun modifyBloodPressure(id: Long, bp: DbBloodPressure): Boolean {
        val logTag = "DbTableSQLite.modifyBloodPressure()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_BLOOD_PRESSURE, arrayOf("$id")) ?: return false
                if (!cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "UPDATE db_blood_pressure " +
                    "SET date=?, systolic=?, diastolic=?, pulse=?, comment=? " +
                    "WHERE id=?"
            val values = arrayOf(
                "${bp.mDate}",
                "${bp.mSystolic}",
                "${bp.mDiastolic}",
                "${bp.mPulse}",
                bp.mComment,
                "$id"
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "update error")
            return false
        }
        setLastModified()
        return true
    }

    override fun getBloodGlucoseNumber(): Int? {
        val logTag = "DbTableSQLite.getBloodGlucoseNumber()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var count: Int? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT COUNT(*) FROM db_blood_glucose", arrayOf()) ?: break
                count = if (cursor.moveToNext()) cursor.getInt(0) else break
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return count
    }

    override fun getBloodGlucoses(): Vector<DbBloodGlucose>? {
        val logTag = "DbTableSQLite.getBloodGlucoses()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var bgList: Vector<DbBloodGlucose>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT * FROM db_blood_glucose ORDER BY date,id", arrayOf())
                        ?: break
                bgList = Vector<DbBloodGlucose>()
                while (cursor.moveToNext()) {
                    val b = DbBloodGlucose(cursor.getLong(COL_IDX_BG_ID))
                    b.mDate = cursor.getLong(COL_IDX_BG_DATE)
                    b.mGlucose = cursor.getDouble(COL_IDX_BG_GLUCOSE)
                    b.mMeal = when (cursor.getInt(COL_IDX_BG_MEAL)) {
                        DbBloodGlucose.Meal.NORMAL.v -> DbBloodGlucose.Meal.NORMAL
                        DbBloodGlucose.Meal.BEFORE.v -> DbBloodGlucose.Meal.BEFORE
                        DbBloodGlucose.Meal.AFTER.v -> DbBloodGlucose.Meal.AFTER
                        else -> continue
                    }
                    b.mComment = cursor.getString(COL_IDX_BG_COMMENT) ?: ""

                    bgList.add(b)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return bgList
    }

    override fun getBloodGlucoses(from: Long, to: Long): Vector<DbBloodGlucose>? {
        val logTag = "DbTableSQLite.getBloodGlucoses(from, to)"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var bgList: Vector<DbBloodGlucose>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery(
                        "SELECT * FROM db_blood_glucose WHERE date BETWEEN ? AND ? " +
                                "ORDER BY date,id",
                        arrayOf("$from", "$to")
                    )
                        ?: break
                bgList = Vector<DbBloodGlucose>()
                while (cursor.moveToNext()) {
                    val b = DbBloodGlucose(cursor.getLong(COL_IDX_BG_ID))
                    b.mDate = cursor.getLong(COL_IDX_BG_DATE)
                    b.mGlucose = cursor.getDouble(COL_IDX_BG_GLUCOSE)
                    b.mMeal = when (cursor.getInt(COL_IDX_BG_MEAL)) {
                        DbBloodGlucose.Meal.NORMAL.v -> DbBloodGlucose.Meal.NORMAL
                        DbBloodGlucose.Meal.BEFORE.v -> DbBloodGlucose.Meal.BEFORE
                        DbBloodGlucose.Meal.AFTER.v -> DbBloodGlucose.Meal.AFTER
                        else -> continue
                    }
                    b.mComment = cursor.getString(COL_IDX_BG_COMMENT) ?: ""

                    bgList.add(b)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return bgList
    }

    override fun getBloodGlucose(id: Long): DbBloodGlucose? {
        val logTag = "DbTableSQLite.getBloodGlucose()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var b: DbBloodGlucose? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_BLOOD_GLUCOSE, arrayOf("$id")) ?: break
                if (!cursor.moveToNext()) {
                    break
                }
                b = DbBloodGlucose(id)
                b.mDate = cursor.getLong(COL_IDX_BG_DATE)
                b.mGlucose = cursor.getDouble(COL_IDX_BG_GLUCOSE)
                b.mMeal = when (cursor.getInt(COL_IDX_BG_MEAL)) {
                    DbBloodGlucose.Meal.NORMAL.v -> DbBloodGlucose.Meal.NORMAL
                    DbBloodGlucose.Meal.BEFORE.v -> DbBloodGlucose.Meal.BEFORE
                    DbBloodGlucose.Meal.AFTER.v -> DbBloodGlucose.Meal.AFTER
                    else -> break
                }
                b.mComment = cursor.getString(COL_IDX_BG_COMMENT) ?: ""
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return b
    }

    override fun addBloodGlucose(bg: DbBloodGlucose): Boolean {
        val logTag = "DbTableSQLite.addBloodGlucose()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery(QUERY_BLOOD_GLUCOSE, arrayOf("${bg.mId}")) ?: return false
                if (cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "INSERT INTO db_blood_glucose " +
                    "(id, date, glucose, meal, comment) " +
                    "VALUES (?, ?, ?, ?, ?)"
            val values = arrayOf(
                "${bg.mId}",
                "${bg.mDate}",
                "${bg.mGlucose}",
                "${bg.mMeal.v}",
                bg.mComment
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "insert error")
            return false
        }
        setLastModified()
        return true
    }

    override fun deleteBloodGlucose(id: Long): Boolean {
        val logTag = "DbTableSQLite.deleteBloodGlucose()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("DELETE FROM db_blood_glucose WHERE id=?", arrayOf(id))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "delete error")
            return false
        }
        setLastModified()
        return true
    }

    override fun modifyBloodGlucose(id: Long, bg: DbBloodGlucose): Boolean {
        val logTag = "DbTableSQLite.modifyBloodGlucose()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_BLOOD_GLUCOSE, arrayOf("$id")) ?: return false
                if (!cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "UPDATE db_blood_glucose " +
                    "SET date=?, glucose=?, meal=?, comment=? " +
                    "WHERE id=?"
            val values = arrayOf(
                "${bg.mDate}",
                "${bg.mGlucose}",
                "${bg.mMeal.v}",
                bg.mComment,
                "$id"
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "update error")
            return false
        }
        setLastModified()
        return true
    }

    private fun createDatabase(): Boolean {
        val logTag = "DbTableSQLite.createDatabase()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return false
        }

        try {
            dbConn?.execSQL(CREATE_DB_INFO)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_info error")
            return false
        }
        try {
            dbConn?.execSQL(CREATE_DB_INFO_IDX)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_info index error")
            return false
        }
        try {
            dbConn?.execSQL(
                "INSERT INTO db_info (name, version, last_modify) VALUES (?, ?, ?)",
                arrayOf(DB_NAME, "$DB_TOP_VERSION", "0")
            )
        } catch (e: SQLiteException) {
            Log.w(logTag, e.message ?: "insert db_info error")
        }
        try {
            dbConn?.execSQL(CREATE_DB_BODY_WEIGHT)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_body_weight error")
            return false
        }
        try {
            dbConn?.execSQL(CREATE_DB_BLOOD_PRESSURE)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_blood_pressure error")
            return false
        }
        try {
            dbConn?.execSQL(CREATE_DB_BLOOD_GLUCOSE)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_blood_glucose error")
            return false
        }
        return true
    }

    private fun updateDatabase(dbVersion: Int): Boolean {
        val logTag = "DbTableSQLite.updateDatabase()"

        if (dbVersion == DB_VERSION_V1) {
            try {
                dbConn?.execSQL("ALTER TABLE db_body_weight RENAME TO db_tmp_bw")
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "db_info error")
                return false
            }
            try {
                dbConn?.execSQL(CREATE_DB_BODY_WEIGHT)
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "db_info error")
                return false
            }
            try {
                dbConn?.execSQL("INSERT INTO db_body_weight (id, date, weight, fat, int_fat, " +
                        "bmi, wc, bone, muscle, water, metabolic, age, comment) " +
                        "SELECT id, date, weight, fat, int_fat, bmi, 0 AS wc, bone, muscle, " +
                        "water, metabolic, age, comment FROM db_tmp_bw"
                )
                dbConn?.execSQL("UPDATE db_info SET version=$DB_TOP_VERSION")
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "db_info error")
                return false
            }
        }
        return true
    }

    override var isReadOnly = false
    override var dbFileName = ""
    private var dbVersion = 0
    private var dbConn: SQLiteDatabase? = null
}

private const val CREATE_DB_INFO: String = "CREATE TABLE IF NOT EXISTS db_info (" +
        "'name' TEXT NOT NULL," +
        "'version' INTEGER NOT NULL," +
        "'descript' TEXT," +
        "'last_modify' INTEGER NOT NULL)"
private const val CREATE_DB_INFO_IDX: String =
    "CREATE UNIQUE INDEX IF NOT EXISTS db_info_idx_name ON db_info (name)"
private const val CREATE_DB_BODY_WEIGHT: String = "CREATE TABLE IF NOT EXISTS db_body_weight (" +
        "'id' INTEGER NOT NULL UNIQUE," +
        "'date' INTEGER NOT NULL," +
        "'weight' REAL NOT NULL," +
        "'fat' REAL NOT NULL," +
        "'int_fat' REAL NOT NULL," +
        "'bmi' REAL NOT NULL," +
        "'wc' REAL NOT NULL," +
        "'bone' REAL NOT NULL," +
        "'muscle' REAL NOT NULL," +
        "'water' REAL NOT NULL," +
        "'metabolic' INTEGER NOT NULL," +
        "'age' INTEGER NOT NULL," +
        "'comment' TEXT," +
        "PRIMARY KEY('id'))"
private const val CREATE_DB_BLOOD_PRESSURE: String =
    "CREATE TABLE IF NOT EXISTS db_blood_pressure (" +
            "'id' INTEGER NOT NULL UNIQUE," +
            "'date' INTEGER NOT NULL," +
            "'systolic' INTEGER NOT NULL," +
            "'diastolic' INTEGER NOT NULL," +
            "'pulse' INTEGER NOT NULL," +
            "'comment' TEXT," +
            "PRIMARY KEY('id'))"
private const val CREATE_DB_BLOOD_GLUCOSE: String =
    "CREATE TABLE IF NOT EXISTS db_blood_glucose (" +
            "'id' INTEGER NOT NULL UNIQUE," +
            "'date' INTEGER NOT NULL," +
            "'glucose' REAL NOT NULL," +
            "'meal' INTEGER NOT NULL," +
            "'comment' TEXT," +
            "PRIMARY KEY('id'))"
private const val QUERY_BODY_WEIGHT: String = "SELECT * FROM db_body_weight WHERE id=?"
private const val QUERY_BLOOD_PRESSURE: String = "SELECT * FROM db_blood_pressure WHERE id=?"
private const val QUERY_BLOOD_GLUCOSE: String = "SELECT * FROM db_blood_glucose WHERE id=?"
