package tw.idv.woofdog.easyhealthrecord.network

import java.io.FileInputStream
import java.io.FileOutputStream

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient

/**
 * This is a class that handles operations of synchronizing FTP.
 */
class NetFtp(isFtps: Boolean = false) {
    enum class Result {
        OK,
        ERR_NETWORK, // Network error.
        ERR_REJECT // Rejected by FTP site.
    }

    /**
     * Login to the FTP site.
     */
    fun login(host: String, port: Int, user: String, password: String): Result {
        var ret: Boolean
        try {
            client.connectTimeout = 5000
            client.connect(host, port)
            client.enterLocalPassiveMode()
            ret = client.login(user, password)
            if (!ret) {
                return Result.ERR_REJECT
            }
            ret = client.setFileType(FTP.BINARY_FILE_TYPE)
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        return if (ret) Result.OK else Result.ERR_REJECT
    }

    /**
     * Disconnect from the FTP site.
     */
    fun disconnect(): Result {
        val ret: Boolean
        try {
            ret = client.logout()
            if (ret && client.isConnected) {
                client.disconnect()
            }
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        return if (ret) Result.OK else Result.ERR_REJECT
    }

    /**
     * Change the working directory.
     *
     * @param dir The path of the directory.
     */
    fun changeDir(dir: String): Result {
        val path = dir.ifEmpty { "." }
        val ret: Boolean
        try {
            ret = client.changeWorkingDirectory(path)
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        return if (ret) Result.OK else Result.ERR_REJECT
    }

    /**
     * Download a file from the FTP site to a local file.
     *
     * @param localFile The local file to write.
     * @param fileName The remote file name.
     */
    fun downloadFile(localFile: FileOutputStream, fileName: String): Result {
        val ret: Boolean
        try {
            ret = client.retrieveFile(fileName, localFile)
            localFile.close()
        } catch (_: Exception) {
            try {
                localFile.close()
            } catch (_: Exception) {
            }
            return Result.ERR_NETWORK
        }
        return if (ret) Result.OK else Result.ERR_REJECT
    }

    /**
     * Delete a file in the FTP site.
     *
     * @param fileName The remote file name.
     */
    fun removeFile(fileName: String): Result {
        val ret: Boolean
        try {
            ret = client.deleteFile(fileName)
        } catch (e: Exception) {
            return Result.ERR_NETWORK
        }
        return if (ret) Result.OK else Result.ERR_REJECT
    }

    /**
     * Rename a file in the FTP site.
     *
     * @param srcFileName The source file name.
     * @param dstFileName The destination file name.
     */
    fun renameFile(srcFileName: String, dstFileName: String): Result {
        val ret: Boolean
        try {
            ret = client.rename(srcFileName, dstFileName)
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        return if (ret) Result.OK else Result.ERR_REJECT
    }

    /**
     * Upload a file in the FTP site.
     *
     * @param localFile The file to upload.
     * @param fileName The remote file name.
     */
    fun uploadFile(localFile: FileInputStream, fileName: String): Result {
        val ret: Boolean
        try {
            ret = client.storeFile(fileName, localFile)
            localFile.close()
        } catch (_: Exception) {
            try {
                localFile.close()
            } catch (_: Exception) {
            }
            return Result.ERR_NETWORK
        }
        return if (ret) Result.OK else Result.ERR_REJECT
    }

    private val client: FTPClient = if (isFtps) FTPSClient() else FTPClient()
}
