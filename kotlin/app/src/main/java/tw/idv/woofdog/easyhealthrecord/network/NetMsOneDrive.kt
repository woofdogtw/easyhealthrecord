package tw.idv.woofdog.easyhealthrecord.network

import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

import tw.idv.woofdog.easyhealthrecord.utils.Utils

/**
 * This is a class that handles operations of synchronizing Microsoft OneDrive.
 */
class NetMsOneDrive {
    /**
     * Hint the program to show a web page to Microsoft authentication page.
     */
    fun interface NeedLoginListener {
        fun onNeedLogin(uri: String)
    }

    /**
     * Hint the program to save the refresh token for future use.
     */
    fun interface TokenGetListener {
        fun onTokenGet(refreshToken: String)
    }

    enum class Result {
        OK,
        ERR_NETWORK, // Network error.
        ERR_REJECT, // Rejected by Microsoft.
        ERR_LOGIN // Need open a web page to login.
    }

    /**
     * To add a listener of NeedLoginListener.
     *
     * @param listener
     */
    fun addNeedLoginListener(listener: NeedLoginListener) {
        needLoginListeners.add(listener)
    }

    /**
     * To remove the listener of NeedLoginListener.
     *
     * @param listener
     */
    fun removeNeedLoginListener(listener: NeedLoginListener) {
        needLoginListeners.remove(listener)
    }

    /**
     * To add a listener of TokenGetListener.
     *
     * @param listener
     */
    fun addTokenGetListener(listener: TokenGetListener) {
        tokenGetListeners.add(listener)
    }

    /**
     * To remove the listener of TokenGetListener.
     *
     * @param listener
     */
    fun removeTokenGetListener(listener: TokenGetListener) {
        tokenGetListeners.remove(listener)
    }

    // For Live Connect API OAuth2.
    /**
     * Set the authorization code from the OAuth2 login page.
     *
     * @param code The authorization code from the OAuth2 login page.
     */
    fun setAuthCode(code: String) {
        authCode = code
    }

    /**
     * Get the refresh token that has been gotten.
     *
     * @return The saved refresh token.
     */
    fun getRefreshToken(): String {
        return refreshToken
    }

    /**
     * Set the refresh token that was gotten by OAuth2 API requests.
     *
     * @param token
     */
    fun setRefreshToken(token: String) {
        refreshToken = token
    }

    /**
     * Get the file ID of the directory.
     *
     * @param dir The path of the directory.
     * @return
     */
    fun changeDir(dir: String): Result {
        val result = refreshAccessToken()
        if (result != Result.OK) {
            return result
        }

        dbDirPath = ""
        for (dirName in dir.split("/")) {
            if (dbDirPath.isNotEmpty()) {
                dbDirPath += "/"
            }
            try {
                dbDirPath += URLEncoder.encode(dirName, "UTF-8")
            } catch (_: Exception) {
                return Result.ERR_REJECT
            }
        }

        if (dbDirPath.isEmpty()) {
            return Result.OK
        }

        return getFileId(dbDirPath).second
    }

    /**
     * Download a file from Microsoft OneDrive to a local file.
     *
     * @param localFile The local file to write.
     * @param fileName The remote file name.
     * @return
     */
    fun downloadFile(localFile: FileOutputStream, fileName: String): Result {
        val result = refreshAccessToken()
        if (result != Result.OK) {
            return result
        }

        var url = "https://graph.microsoft.com/v1.0/me/drive/root:/"
        if (dbDirPath.isNotEmpty()) {
            url += "$dbDirPath/"
        }
        try {
            url += URLEncoder.encode(fileName, "UTF-8") + ":/content"
        } catch (_: Exception) {
            return Result.ERR_REJECT
        }

        val client = OkHttpClient()
        val req =
            Request.Builder().url(url).header("Authorization", "Bearer $accessToken").get().build()
        try {
            val res = client.newCall(req).execute()
            if (res.code != 200) {
                return Result.ERR_REJECT
            }
            (res.body ?: throw Exception()).byteStream().copyTo(localFile)
        } catch (_: Exception) {
            try {
                localFile.close()
            } catch (_: Exception) {
            }
            return Result.ERR_NETWORK
        }
        return Result.OK
    }

    /**
     * Delete a file in Microsoft OneDrive.
     *
     * @param fileName The remote file name.
     * @return
     */
    fun removeFile(fileName: String): Result {
        val result = refreshAccessToken()
        if (result != Result.OK) {
            return result
        }

        var filePath = dbDirPath
        if (filePath.isNotEmpty()) {
            filePath += "/"
        }
        try {
            filePath += URLEncoder.encode(fileName, "UTF-8")
        } catch (_: Exception) {
            return Result.ERR_REJECT
        }
        val fileIdResult = getFileId(filePath)
        if (fileIdResult.second != Result.OK) {
            return fileIdResult.second
        }

        val url = "https://graph.microsoft.com/v1.0/me/drive/items/${fileIdResult.first}"

        val client = OkHttpClient()
        val req = Request.Builder().url(url).header("Authorization", "Bearer $accessToken").delete()
            .build()
        val res = try {
            client.newCall(req).execute()
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        return if (res.code == 204) Result.OK else Result.ERR_REJECT
    }

    /**
     * Rename a file in Microsoft OneDrive.
     *
     * @param srcFileName The source file name.
     * @param dstFileName The destination file name.
     * @return
     */
    fun renameFile(srcFileName: String, dstFileName: String): Result {
        val result = refreshAccessToken()
        if (result != Result.OK) {
            return result
        }

        // Get the file ID and parent ID first.
        val dirIdResult = getFileId(dbDirPath)
        if (dirIdResult.second != Result.OK) {
            return dirIdResult.second
        }
        var filePath = dbDirPath
        if (filePath.isNotEmpty()) {
            filePath += "/"
        }
        try {
            filePath += URLEncoder.encode(srcFileName, "UTF-8")
        } catch (_: Exception) {
            return Result.ERR_REJECT
        }
        val fileIdResult = getFileId(filePath)
        if (fileIdResult.second != Result.OK) {
            return fileIdResult.second
        }

        // Generate the JSON parameters to PATCH the new file name.
        val patchData =
            """{"parentReference":{"id":"${dirIdResult.first}"},"name":"$dstFileName"}"""
        val body = patchData.toRequestBody("application/json".toMediaType())
        val url = "https://graph.microsoft.com/v1.0/me/drive/items/${fileIdResult.first}"

        val client = OkHttpClient()
        val req =
            Request.Builder().url(url).header("Authorization", "Bearer $accessToken").patch(body)
                .build()
        val res = try {
            client.newCall(req).execute()
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        return if (res.code == 200) Result.OK else Result.ERR_REJECT
    }

    /**
     * Upload a file in Microsoft OneDrive.
     *
     * @param localFile The file to upload.
     * @param fileName The remote file name.
     * @return
     */
    fun uploadFile(localFile: File, fileName: String): Result {
        val result = refreshAccessToken()
        if (result != Result.OK) {
            return result
        }

        // Get the parent ID first.
        val dirIdResult = getFileId(dbDirPath)
        if (dirIdResult.second != Result.OK) {
            return dirIdResult.second
        }
        val url =
            "https://graph.microsoft.com/v1.0/me/drive/items/${dirIdResult.first}:/$fileName:/content"
        val body = localFile.asRequestBody(null)

        val client = OkHttpClient()
        val req =
            Request.Builder().url(url).header("Authorization", "Bearer $accessToken").put(body)
                .build()
        val res = try {
            client.newCall(req).execute()
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        return if (res.code == 201) Result.OK else Result.ERR_REJECT
    }

    /**
     * Get file ID.
     *
     * @param filePath The file path in OneDrive.
     * @return The string is the file ID if Result value is OK.
     */
    private fun getFileId(filePath: String): Pair<String, Result> {
        val client = OkHttpClient()
        val req =
            Request.Builder().url("https://graph.microsoft.com/v1.0/me/drive/root:/$filePath")
                .header("Authorization", "Bearer $accessToken").get().build()
        val res = try {
            client.newCall(req).execute()
        } catch (_: Exception) {
            return Pair("", Result.ERR_NETWORK)
        }
        if (res.code != 200) {
            return Pair("", Result.ERR_REJECT)
        }

        val jsonObj = try {
            JSONObject((res.body ?: throw Exception()).string())
        } catch (_: Exception) {
            return Pair("", Result.ERR_NETWORK)
        }
        val fileId = jsonObj.getString("id")
        return if (fileId.isNotEmpty()) Pair(fileId, Result.OK) else Pair("", Result.ERR_REJECT)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decryptAes(hexStr: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivParameterSpec = IvParameterSpec(MS_OAUTH2_AES_IV.toByteArray(), 0, 16)
        val keySpec = SecretKeySpec(MS_OAUTH2_AES_KEY.toByteArray(), 0, 16, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec)
        val decrypt = cipher.doFinal(hexStr.hexToByteArray())
        return String(decrypt)
    }

    // Refresh the access token if it is expired or it is not existing.
    private fun refreshAccessToken(): Result {
        if (Utils.getCurrentTimeEpoch() / 1000 >= expired) {
            accessToken = ""
        }
        if (accessToken.isNotEmpty()) {
            return Result.OK
        }

        if (refreshToken.isEmpty()) {
            if (authCode.isEmpty()) {
                // Hint the user program to create a Login web page to get an authorization code.
                if (needLoginListeners.size > 0) {
                    val uri = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?" +
                            "client_id=${decryptAes(MS_OAUTH2_CLIENT_ID)}" +
                            "&scope=${MS_OAUTH2_SCOPE}" +
                            "&redirect_uri=${MS_OAUTH2_REDIRECT_URI}" +
                            "&response_type=code"
                    for (listener in needLoginListeners) {
                        listener.onNeedLogin(uri)
                    }
                }
                return Result.ERR_LOGIN
            } else {
                // Get the access token and the refresh token using the authorization code.
                val postData = "client_id=${decryptAes(MS_OAUTH2_CLIENT_ID)}" +
                        "&redirect_uri=${MS_OAUTH2_REDIRECT_URI}" +
                        "&code=$authCode" +
                        "&grant_type=authorization_code"
                return oAuth2RequestPost(postData)
            }
        } else {
            // Get the access token using the refresh token.
            val postData = "client_id=${decryptAes(MS_OAUTH2_CLIENT_ID)}" +
                    "&redirect_uri=${MS_OAUTH2_REDIRECT_URI}" +
                    "&refresh_token=$refreshToken" +
                    "&grant_type=refresh_token"
            return oAuth2RequestPost(postData)
        }
    }

    private fun oAuth2RequestPost(postData: String): Result {
        val client = OkHttpClient()
        val body = postData.toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val req =
            Request.Builder().url("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
                .post(body).build()
        val res = try {
            client.newCall(req).execute()
        } catch (e: Exception) {
            return Result.ERR_NETWORK
        }
        if (res.code != 200) {
            return Result.ERR_REJECT
        }

        val jsonObj = try {
            JSONObject((res.body ?: throw Exception()).string())
        } catch (_: Exception) {
            return Result.ERR_NETWORK
        }
        try {
            accessToken = jsonObj.getString("access_token")
        } catch (_: Exception) {
        }
        try {
            refreshToken = jsonObj.getString("refresh_token")
            for (listener in tokenGetListeners) {
                listener.onTokenGet(refreshToken)
            }
        } catch (_: Exception) {
        }
        try {
            expired = Utils.getCurrentTimeEpoch() / 1000 + jsonObj.getInt("expires_in")
        } catch (_: Exception) {
        }
        return Result.OK
    }

    companion object {
        // Microsoft Graph API Console information for this program.
        // These variables are AES encrypted hexadecimal strings before compiling the program.
        private const val MS_OAUTH2_AES_KEY = ""
        private const val MS_OAUTH2_AES_IV = ""
        private const val MS_OAUTH2_SCOPE = "offline_access%20files.readwrite"
        private const val MS_OAUTH2_REDIRECT_URI =
            "https://login.microsoftonline.com/common/oauth2/nativeclient"
        private const val MS_OAUTH2_CLIENT_ID = ""
    }

    // Code and tokens for accessing Microsoft Graph APIs.
    private var authCode = ""
    private var accessToken = ""
    private var refreshToken = ""
    private var expired = 0L

    // The database directory in the OneDrive. This value can be changed by `changeDir()`.
    // Empty value is the root directory.
    private var dbDirPath = ""

    // Handlers.
    private val needLoginListeners = mutableListOf<NeedLoginListener>()
    private val tokenGetListeners = mutableListOf<TokenGetListener>()
}
