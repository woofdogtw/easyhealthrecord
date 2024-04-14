package tw.idv.woofdog.easyhealthrecord.db

import java.io.Serializable

import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * The class to manage all data and operations for body weight.
 *
 * Here are special fields for records:
 * - Record ID:
 *     This is the unique ID to identify records. We use the creation epoch time (from 1970 Jan 1st
 *     0:00:00 UTC in seconds) because no human can create two records in one second.
 * - Date:
 *     This is the decimal number to indicate the date in YYYYMMDDhhmmss format.
 */
open class DbBodyWeight: Cloneable, Serializable {
    constructor(id: Long = Utils.getCurrentTimeEpoch()) {
        mId = id
    }

    constructor(rhs: DbBodyWeight) {
        mId = rhs.mId
        copyFrom(rhs)
    }

    /** Copy whole data except the record ID. */
    fun copyFrom(rhs: DbBodyWeight) {
        this.mDate = rhs.mDate
        this.mWeight = rhs.mWeight
        this.mFat = rhs.mFat
        this.mIntFat = rhs.mIntFat
        this.mBmi = rhs.mBmi
        this.mWc = rhs.mWc
        this.mBone = rhs.mBone
        this.mMuscle = rhs.mMuscle
        this.mWater = rhs.mWater
        this.mMetabolic = rhs.mMetabolic
        this.mAge = rhs.mAge
        this.mComment = rhs.mComment
    }

    var mId = Utils.getCurrentTimeEpoch()
    var mDate = 0L
    var mWeight = 0.0
    var mFat = 0.0
    var mIntFat = 0.0
    var mBmi = 0.0
    var mWc = 0.0
    var mBone = 0.0
    var mMuscle = 0.0
    var mWater = 0.0
    var mMetabolic = 0
    var mAge = 0
    var mComment = ""
}
