package kz.gov.mia.sos.widget.webview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar

class WebViewActivity : AppCompatActivity(), WebView.Listener {

    companion object {
        private val TAG = WebViewActivity::class.java.simpleName

        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private var appBarLayout: AppBarLayout? = null
    private var toolbar: MaterialToolbar? = null
    private var webView: WebView? = null

    private val commonPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "registerForActivityResult() -> permissions: $permissions")

            webView?.setPermissionRequestResult(
                PermissionRequestMapper.fromAndroidToWebClient(permissions)
            )

            if (permissions.any { !it.value }) {
                showRequestPermissionsAlertDialog()
            }
        }

    private val locationPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "registerForActivityResult() -> permissions: $permissions")

            val isAllPermissionsGranted = permissions.all { it.value }

            webView?.setGeolocationPermissionsShowPromptResult(isAllPermissionsGranted)

            if (!isAllPermissionsGranted) {
                showRequestPermissionsAlertDialog()
            }
        }

    private val locationSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "ActivityResultContracts.StartActivityForResult() -> ${result.resultCode}")

            onGeolocationPermissionsShowPrompt()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appBarLayout = findViewById(R.id.appBarLayout)
        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.webView)

        setupActionBar()
        setupWebView()

        webView?.loadUrl("https://kenes.vlx.kz/sos")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webview, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.reload -> {
                AlertDialog.Builder(this, R.style.SOSWidgetWebViewWebView_AlertDialogTheme)
                    .setTitle("Внимание")
                    .setMessage("Вы действительно хотите обновить виджет?")
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Обновить виджет") { dialog, _ ->
                        dialog.dismiss()
                        webView?.loadUrl("javascript:window.location.reload(true)")
                    }
                    .show()
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }
    }

    private fun setupActionBar() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        setupActionBar(toolbar, isBackButtonEnabled = true) {
            AlertDialog.Builder(this, R.style.SOSWidgetWebViewWebView_AlertDialogTheme)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage("Вы действительно хотите выйти из виджета?")
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Выход") { dialog, _ ->
                    dialog.dismiss()
                    onBackPressed()
                }
                .show()
        }
    }

    private fun setupWebView() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            webView?.settings?.let {
                WebSettingsCompat.setForceDark(
                    it,
                    WebSettingsCompat.FORCE_DARK_OFF
                )
            }
        }

        webView?.init()
        webView?.setupCookieManager()
        webView?.setMixedContentAllowed(true)
        webView?.setUrlListener(object : WebView.UrlListener {
            override fun onLoadUrl(headers: Map<String, String>?, url: Uri) {
            }
        })
        webView?.setListener(this)
    }

    private fun showRequestPermissionsAlertDialog() {
        AlertDialog.Builder(this, R.style.SOSWidgetWebViewWebView_AlertDialogTheme)
            .setCancelable(false)
            .setTitle("Доступ к разрешениям")
            .setMessage("Пожалуйста, предоставьте разрешения для полноценной работы виджета")
            .setPositiveButton("К настройкам") { dialog, _ ->
                dialog.dismiss()

                startActivity(
                    Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
            .show()
    }

    private fun showGPSDisabledErrorAlertDialog() {
        AlertDialog.Builder(this, R.style.SOSWidgetWebViewWebView_AlertDialogTheme)
            .setCancelable(false)
            .setTitle("Доступ к местоположению")
            .setMessage("Пожалуйста, включите функцию \"Местоположение\" для совершения звонка")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()

                locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .show()

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
        commonPermissionsLauncher.launch(permissions)
    }

    override fun onPermissionRequestCanceled(resources: Array<String>) {
        Log.d(TAG, "onPermissionRequestCanceled() -> resources: ${resources.contentToString()}")
    }

    override fun onGeolocationPermissionsShowPrompt() {
        Log.d(TAG, "onGeolocationPermissionsShowPrompt()")
        if (LOCATION_PERMISSIONS.all {
                ActivityCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            val locationManager = ContextCompat.getSystemService(this, LocationManager::class.java)
            if (locationManager == null) {
                showGPSDisabledErrorAlertDialog()
            } else {
                if (LocationManagerCompat.isLocationEnabled(locationManager)) {
                    webView?.setGeolocationPermissionsShowPromptResult(true)
                } else {
                    showGPSDisabledErrorAlertDialog()
                }
            }
        } else {
            locationPermissionsLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    override fun onGeolocationPermissionsHidePrompt() {
        Log.d(TAG, "onGeolocationPermissionsHidePrompt()")
    }

}