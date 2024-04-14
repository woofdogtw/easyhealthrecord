package tw.idv.woofdog.easyhealthrecord.db

import java.util.Calendar

import tw.idv.woofdog.easyhealthrecord.utils.Utils
import java.util.Vector

/**
 * The base class of all database.
 *
 * With this class, the program can manage databases without knowing detail implementation.
 *   - The getter function returns `null` means that the get operation has errors.
 *   - The setter function returns `false` means that the set operation has errors.
 *
 * The READ ONLY attribute is used for supporting old database formats.
 *
 * Please refer to `doc/schema.md` to get detail information of database tables.
 */
interface DbTableBase {
    companion object {
        fun getDateFromYMDhms(year: Int, month: Int, day: Int): Long {
            return getDateFromYMDhms(year, month, day, 0, 0, 0)
        }

        fun getDateFromYMDhms(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: Int
        ): Long {
            return year.toLong() * 10000000000L +
                    month.toLong() * 100000000L +
                    day.toLong() * 1000000 +
                    hour.toLong() * 10000 +
                    minute.toLong() * 100 +
                    second.toLong()
        }

        fun getYearFromDate(date: Long): Int {
            return (date / 10000000000L).toInt()
        }

        fun getMonthFromDate(date: Long): Int {
            return (date / 100000000L % 100).toInt()
        }

        fun getDayFromDate(date: Long): Int {
            return (date / 1000000 % 100).toInt()
        }

        fun getHourFromDate(date: Long): Int {
            return (date / 10000 % 100).toInt()
        }

        fun getMinuteFromDate(date: Long): Int {
            return (date / 100 % 100).toInt()
        }

        fun getSecondFromDate(date: Long): Int {
            return (date % 100).toInt()
        }

        fun getCalendarFromDate(date: Long): Calendar {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, getYearFromDate(date))
            calendar.set(Calendar.MONTH, getMonthFromDate(date) - 1)
            calendar.set(Calendar.DATE, getDayFromDate(date))
            calendar.set(Calendar.HOUR_OF_DAY, getHourFromDate(date))
            calendar.set(Calendar.MINUTE, getMinuteFromDate(date))
            calendar.set(Calendar.SECOND, getSecondFromDate(date))
            return calendar
        }

        fun getDateFromCalendar(calendar: Calendar): Long {
            return getDateFromYMDhms(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DATE),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )
        }
    }

    // Get functions.
    fun getFileName(): String {
        return dbFileName
    }

    fun getDescription(): String?
    fun getLastModified(): Long?

    // Set functions.
    fun setFileName(name: String): Boolean
    fun setDescription(description: String): Boolean
    fun setLastModified(lastTime: Long = Utils.getCurrentTimeEpoch()): Boolean

    // Body weight operation functions.
    fun getBodyWeightNumber(): Int?
    fun getBodyWeights(): Vector<DbBodyWeight>?
    fun getBodyWeights(from: Long, to: Long): Vector<DbBodyWeight>?
    fun getBodyWeight(id: Long): DbBodyWeight?
    fun addBodyWeight(bw: DbBodyWeight): Boolean
    fun deleteBodyWeight(id: Long): Boolean
    fun modifyBodyWeight(id: Long, bw: DbBodyWeight): Boolean

    // Blood pressure operation functions.
    fun getBloodPressureNumber(): Int?
    fun getBloodPressures(): Vector<DbBloodPressure>?
    fun getBloodPressures(from: Long, to: Long): Vector<DbBloodPressure>?
    fun getBloodPressure(id: Long): DbBloodPressure?
    fun addBloodPressure(bp: DbBloodPressure): Boolean
    fun deleteBloodPressure(id: Long): Boolean
    fun modifyBloodPressure(id: Long, bp: DbBloodPressure): Boolean

    // Blood glucose operation functions.
    fun getBloodGlucoseNumber(): Int?
    fun getBloodGlucoses(): Vector<DbBloodGlucose>?
    fun getBloodGlucoses(from: Long, to: Long): Vector<DbBloodGlucose>?
    fun getBloodGlucose(id: Long): DbBloodGlucose?
    fun addBloodGlucose(bg: DbBloodGlucose): Boolean
    fun deleteBloodGlucose(id: Long): Boolean
    fun modifyBloodGlucose(id: Long, bg: DbBloodGlucose): Boolean

    var isReadOnly: Boolean
    var dbFileName: String
}
