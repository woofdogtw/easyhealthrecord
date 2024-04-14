package tw.idv.woofdog.easyhealthrecord.db

import java.io.Serializable

import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The class to manage all data and operations for blood glucose.
 *
 * Here are special fields for records:
 * - Record ID:
 *     This is the unique ID to identify records. We use the creation epoch time (from 1970 Jan 1st
 *     0:00:00 UTC in seconds) because no human can create two records in one second.
 * - Date:
 *     This is the decimal number to indicate the date in YYYYMMDDhhmmss format.
 */
open class DbBloodGlucose: Cloneable, Serializable {
    enum class Meal(val v: Int) {
        NORMAL(0), BEFORE(1), AFTER(2)
    }

    constructor(id: Long = Utils.getCurrentTimeEpoch()) {
        mId = id
    }

    constructor(rhs: DbBloodGlucose) {
        mId = rhs.mId
        copyFrom(rhs)
    }

    /** Copy whole data except the record ID. */
    fun copyFrom(rhs: DbBloodGlucose) {
        this.mDate = rhs.mDate
        this.mGlucose = rhs.mGlucose
        this.mMeal = rhs.mMeal
        this.mComment = rhs.mComment
    }

    var mId = Utils.getCurrentTimeEpoch()
    var mDate = 0L
    var mGlucose = 0.0
    var mMeal = Meal.NORMAL
    var mComment = ""
}
