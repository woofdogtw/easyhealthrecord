package tw.idv.woofdog.easyhealthrecord.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

import tw.idv.woofdog.easyhealthrecord.R

/**
 * The activity for user to login the Microsoft service.
 */
class SyncLoginMsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_login)
        supportActionBar?.hide()

        val view = findViewById<WebView>(R.id.syncLoginWebView)
        view.webViewClient = client
        val settings = view.settings
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.javaScriptEnabled = true

        intent.getStringExtra("uri")?.let {
            view.loadUrl(it)
        }
    }

    private val client = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            url?.let {
                val code = Uri.parse(it).getQueryParameter("code")
                code?.let{
                    val bundle = Bundle()
                    bundle.putString("code", code)
                    bundle.putString("type", "ms")
                    val data = Intent()
                    data.putExtras(bundle)
                    this@SyncLoginMsActivity.setResult(0, data)
                    this@SyncLoginMsActivity.finish()
                }
            }
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            request?.url?.let { view?.loadUrl(it.toString()) }
            return true
        }
    }
}
