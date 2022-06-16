package kz.gov.mia.sos.widget.webview

import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity(), WebView.Listener {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private var webView: WebView? = null

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "registerForActivityResult() -> permissions: $permissions")

            webView?.setPermissionRequestResult(
                PermissionRequestMapper.fromAndroidToWebClient(permissions)
            )

            if (permissions.any { !it.value }) {
                AlertDialog.Builder(this)
                    .setTitle("Grant permissions")
                    .setMessage("Please, provide with permissions")
                    .setPositiveButton("To Settings") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            webView?.settings?.let {
                WebSettingsCompat.setForceDark(
                    it,
                    WebSettingsCompat.FORCE_DARK_OFF
                )
            }
        }

        setupWebView()

        webView?.loadUrl("https://kenes.vlx.kz/sos")

//        launcher.launch(
//            arrayOf(
//                Manifest.permission.CAMERA,
//                Manifest.permission.RECORD_AUDIO,
//                Manifest.permission.MODIFY_AUDIO_SETTINGS,
//            )
//        )
    }

    private fun setupWebView() {
        webView?.init()
        webView?.setupCookieManager()
        webView?.setMixedContentAllowed(true)
        webView?.setUrlListener(object : WebView.UrlListener {
            override fun onLoadUrl(headers: Map<String, String>?, url: Uri) {
            }
        })
        webView?.setListener(this)
    }

    override fun onReceivedSSLError(handler: SslErrorHandler?, error: SslError?) {
    }

    override fun onPageLoadProgress(progress: Int) {
    }

    override fun onSelectFileRequested(filePathCallback: ValueCallback<Array<Uri>>?): Boolean {
        return false
    }

    override fun onPermissionRequest(resources: Array<String>) {
        val permissions = PermissionRequestMapper.fromWebClientToAndroid(resources).toTypedArray()
        Log.d(TAG, "onPermissionRequest() -> resources: ${resources.contentToString()}")
        Log.d(TAG, "onPermissionRequest() -> permissions: ${permissions.contentToString()}")
        launcher.launch(permissions)
    }

    override fun onPermissionRequestCanceled(resources: Array<String>) {
        Log.d(TAG, "onPermissionRequestCanceled() -> resources: ${resources.contentToString()}")
    }

}