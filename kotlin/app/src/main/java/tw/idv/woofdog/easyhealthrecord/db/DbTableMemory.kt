package tw.idv.woofdog.easyhealthrecord.db

import java.util.Vector

/**
 * The memory implementation of DbTableBase.
 *
 * This class implements complete database operations for this program, but all data are in memory.
 *
 * This class is used for handling volatile database, such as find result and importing other
 * database format data.
 */
class DbTableMemory : DbTableBase {
    override fun getDescription(): String? {
        return dbDescription
    }

    override fun getLastModified(): Long? {
        return dbLastModified
    }

    override fun setFileName(name: String): Boolean {
        return false
    }

    override fun setDescription(description: String): Boolean {
        if (isReadOnly) {
            return false
        }

        dbDescription = description
        return true
    }

    override fun setLastModified(lastTime: Long): Boolean {
        if (isReadOnly) {
            return false
        }

        dbLastModified = lastTime
        return true
    }

    override fun getBodyWeightNumber(): Int? {
        return dbBodyWeights.count()
    }

    override fun getBodyWeights(): Vector<DbBodyWeight>? {
        val list = Vector<DbBodyWeight>(dbBodyWeights.count())
        for (b in dbBodyWeights) {
            list.add(DbBodyWeight(b))
        }
        return list
    }

    override fun getBodyWeights(from: Long, to: Long): Vector<DbBodyWeight>? {
        val list = Vector<DbBodyWeight>(dbBodyWeights.count())
        for (b in dbBodyWeights) {
            if (b.mDate in from..to) {
                list.add(DbBodyWeight(b))
            }
        }
        return list
    }

    override fun getBodyWeight(id: Long): DbBodyWeight? {
        for (b in dbBodyWeights) {
            if (b.mId == id) {
                return DbBodyWeight(b)
            }
        }
        return null
    }

    override fun addBodyWeight(bw: DbBodyWeight): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBodyWeights) {
            if (b.mId == bw.mId) {
                return false
            }
        }
        dbBodyWeights.add(DbBodyWeight(bw))
        setLastModified()
        return true
    }

    override fun deleteBodyWeight(id: Long): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBodyWeights) {
            if (b.mId == id) {
                dbBodyWeights.remove(b)
                return true
            }
        }
        return true
    }

    override fun modifyBodyWeight(id: Long, bw: DbBodyWeight): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBodyWeights) {
            if (b.mId == id) {
                b.copyFrom(bw)
                return true
            }
        }
        return false
    }

    override fun getBloodPressureNumber(): Int? {
        return dbBloodPressures.count()
    }

    override fun getBloodPressures(): Vector<DbBloodPressure>? {
        val list = Vector<DbBloodPressure>(dbBloodPressures.count())
        for (b in dbBloodPressures) {
            list.add(DbBloodPressure(b))
        }
        return list
    }

    override fun getBloodPressures(from: Long, to: Long): Vector<DbBloodPressure>? {
        val list = Vector<DbBloodPressure>(dbBloodPressures.count())
        for (b in dbBloodPressures) {
            if (b.mDate in from..to) {
                list.add(DbBloodPressure(b))
            }
        }
        return list
    }

    override fun getBloodPressure(id: Long): DbBloodPressure? {
        for (b in dbBloodPressures) {
            if (b.mId == id) {
                return DbBloodPressure(b)
            }
        }
        return null
    }

    override fun addBloodPressure(bp: DbBloodPressure): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBloodPressures) {
            if (b.mId == bp.mId) {
                return false
            }
        }
        dbBloodPressures.add(DbBloodPressure(bp))
        setLastModified()
        return true
    }

    override fun deleteBloodPressure(id: Long): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBloodPressures) {
            if (b.mId == id) {
                dbBloodPressures.remove(b)
                return true
            }
        }
        return true
    }

    override fun modifyBloodPressure(id: Long, bp: DbBloodPressure): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBloodPressures) {
            if (b.mId == id) {
                b.copyFrom(bp)
                return true
            }
        }
        return false
    }

    override fun getBloodGlucoseNumber(): Int? {
        return dbBloodGlucoses.count()
    }

    override fun getBloodGlucoses(): Vector<DbBloodGlucose>? {
        val list = Vector<DbBloodGlucose>(dbBloodGlucoses.count())
        for (b in dbBloodGlucoses) {
            list.add(DbBloodGlucose(b))
        }
        return list
    }

    override fun getBloodGlucoses(from: Long, to: Long): Vector<DbBloodGlucose>? {
        val list = Vector<DbBloodGlucose>(dbBloodGlucoses.count())
        for (b in dbBloodGlucoses) {
            if (b.mDate in from..to) {
                list.add(DbBloodGlucose(b))
            }
        }
        return list
    }

    override fun getBloodGlucose(id: Long): DbBloodGlucose? {
        for (b in dbBloodGlucoses) {
            if (b.mId == id) {
                return DbBloodGlucose(b)
            }
        }
        return null
    }

    override fun addBloodGlucose(bp: DbBloodGlucose): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBloodGlucoses) {
            if (b.mId == bp.mId) {
                return false
            }
        }
        dbBloodGlucoses.add(DbBloodGlucose(bp))
        setLastModified()
        return true
    }

    override fun deleteBloodGlucose(id: Long): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBloodGlucoses) {
            if (b.mId == id) {
                dbBloodGlucoses.remove(b)
                return true
            }
        }
        return true
    }

    override fun modifyBloodGlucose(id: Long, bp: DbBloodGlucose): Boolean {
        if (isReadOnly) {
            return false
        }

        for (b in dbBloodGlucoses) {
            if (b.mId == id) {
                b.copyFrom(bp)
                return true
            }
        }
        return false
    }

    override var isReadOnly = false
    override var dbFileName = ""
    private var dbDescription = ""
    private var dbLastModified = 0L
    private var dbBodyWeights = Vector<DbBodyWeight>()
    private var dbBloodPressures = Vector<DbBloodPressure>()
    private var dbBloodGlucoses = Vector<DbBloodGlucose>()
}
