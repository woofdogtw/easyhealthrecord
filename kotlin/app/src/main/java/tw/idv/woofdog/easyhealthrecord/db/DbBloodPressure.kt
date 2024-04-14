package tw.idv.woofdog.easyhealthrecord.db

import java.io.Serializable

import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The class to manage all data and operations for blood pressure.
 *
 * Here are special fields for records:
 * - Record ID:
 *     This is the unique ID to identify records. We use the creation epoch time (from 1970 Jan 1st
 *     0:00:00 UTC in seconds) because no human can create two records in one second.
 * - Date:
 *     This is the decimal number to indicate the date in YYYYMMDDhhmmss format.
 */
open class DbBloodPressure: Cloneable, Serializable {
    constructor(id: Long = Utils.getCurrentTimeEpoch()) {
        mId = id
    }

    constructor(rhs: DbBloodPressure) {
        mId = rhs.mId
        copyFrom(rhs)
    }

    /** Copy whole data except the record ID. */
    fun copyFrom(rhs: DbBloodPressure) {
        this.mDate = rhs.mDate
        this.mSystolic = rhs.mSystolic
        this.mDiastolic = rhs.mDiastolic
        this.mPulse = rhs.mPulse
        this.mComment = rhs.mComment
    }

    var mId = Utils.getCurrentTimeEpoch()
    var mDate = 0L
    var mSystolic = 0
    var mDiastolic = 0
    var mPulse = 0
    var mComment = ""
}
