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
import android.widget.Toast
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
import kz.gov.mia.sos.widget.webview.multimedia.preview.ImagePreviewDialogFragment
import kz.gov.mia.sos.widget.webview.multimedia.preview.VideoPreviewDialogFragment
import kz.gov.mia.sos.widget.webview.multimedia.selection.GetContentDelegate
import kz.gov.mia.sos.widget.webview.multimedia.selection.GetContentResultContract
import kz.gov.mia.sos.widget.webview.multimedia.selection.MimeType
import kz.gov.mia.sos.widget.webview.multimedia.selection.StorageAccessFrameworkInteractor
import kz.gov.mia.sos.widget.webview.ui.components.ProgressView
import kz.gov.mia.sos.widget.webview.utils.setupActionBar

class WebViewActivity : AppCompatActivity(), WebView.Listener {

    companion object {
        private val TAG = WebViewActivity::class.java.simpleName

        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private val STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private var appBarLayout: AppBarLayout? = null
    private var toolbar: MaterialToolbar? = null
    private var webView: WebView? = null
    private var progressView: ProgressView? = null

    private var interactor: StorageAccessFrameworkInteractor? = null

    private val requestedPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "requestedPermissionsLauncher() -> permissions: $permissions")

            webView?.setPermissionRequestResult(
                PermissionRequestMapper.fromAndroidToWebClient(permissions)
            )

            if (permissions.any { !it.value }) {
                showRequestPermissionsAlertDialog()
            }
        }

    private val locationPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "locationPermissionsLauncher() -> permissions: $permissions")

            val isAllPermissionsGranted = permissions.all { it.value }

            webView?.setGeolocationPermissionsShowPromptResult(isAllPermissionsGranted)

            if (!isAllPermissionsGranted) {
                showRequestPermissionsAlertDialog()
            }
        }

    private val locationSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(TAG, "locationSettingsLauncher() -> resultCode: ${result.resultCode}")

            onGeolocationPermissionsShowPrompt()
        }

    private val storagePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "storagePermissionsLauncher() -> permissions: $permissions")

            val isAllPermissionsGranted = permissions.all { it.value }

            if (isAllPermissionsGranted) {
                onSelectFileRequest()
            } else {
                showRequestPermissionsAlertDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        appBarLayout = findViewById(R.id.appBarLayout)
        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.webView)
        progressView = findViewById(R.id.progressView)

        setupActionBar()
        setupWebView()

        interactor = StorageAccessFrameworkInteractor(this) { result ->
            when (result) {
                is GetContentDelegate.Result.Success -> {
                    webView?.setFileSelectionPromptResult(result.uri)
                }
                is GetContentDelegate.Result.Error.NullableUri -> {
                    Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show()
                }
                is GetContentDelegate.Result.Error.SizeLimitExceeds -> {
                    Toast.makeText(
                        this,
                        "Извините, но вы превысили лимит (${result.maxSize}) при выборе файла",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    Toast.makeText(this, "Произошла ошибка", Toast.LENGTH_SHORT).show()
                }
            }
        }

        webView?.loadUrl("https://kenes.vlx.kz/sos")
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments

        val imagePreviewDialogFragments =
            fragments.filterIsInstance<ImagePreviewDialogFragment>()
        val videoPreviewDialogFragments =
            fragments.filterIsInstance<VideoPreviewDialogFragment>()

        when {
            imagePreviewDialogFragments.isNotEmpty() -> {
                imagePreviewDialogFragments.forEach {
                    it.dismiss()
                    supportFragmentManager.fragments.remove(it)
                }
            }
            videoPreviewDialogFragments.isNotEmpty() -> {
                videoPreviewDialogFragments.forEach {
                    it.dismiss()
                    supportFragmentManager.fragments.remove(it)
                }
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webview, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.reload -> {
                AlertDialog.Builder(this, R.style.SOSWidgetWebViewWebView_AlertDialogTheme)
                    .setTitle("Обновление виджета")
                    .setMessage("Вы действительно хотите обновить виджет?")
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Обновить") { dialog, _ ->
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

    override fun onDestroy() {
        interactor?.dispose()
        interactor = null

        super.onDestroy()
    }

    private fun setupActionBar() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        setupActionBar(toolbar, isBackButtonEnabled = true) {
            AlertDialog.Builder(this, R.style.SOSWidgetWebViewWebView_AlertDialogTheme)
                .setTitle("Выход из виджета")
                .setMessage("Вы действительно хотите выйти из виджета?")
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Выйти") { dialog, _ ->
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
        webView?.setUrlListener { headers, uri ->
            Log.d(TAG, "setUrlListener() -> $headers, $uri")

            if (uri.toString().contains("image")) {
                ImagePreviewDialogFragment.show(
                    fragmentManager = supportFragmentManager,
                    uri = uri,
                    caption = uri.toString()
                )
                return@setUrlListener true
            } else if (uri.toString().contains("video")) {
                VideoPreviewDialogFragment.show(
                    fragmentManager = supportFragmentManager,
                    uri = uri,
                    caption = uri.toString()
                )
            }

            return@setUrlListener false
        }

        webView?.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            Log.d(
                TAG,
                "onDownloadStart() -> " +
                        "url: $url, " +
                        "userAgent: $userAgent, " +
                        "contentDisposition: $contentDisposition, " +
                        "mimetype: $mimetype, " +
                        "contentLength: $contentLength"
            )

            if (mimetype?.startsWith("image") == true &&
                (url.endsWith("png") ||
                        url.endsWith("jpg") ||
                        url.endsWith("jpeg"))
            ) {
                ImagePreviewDialogFragment.show(
                    fragmentManager = supportFragmentManager,
                    uri = Uri.parse(url),
                    caption = null
                )
                return@setDownloadListener
            } else if (mimetype?.startsWith("video") == true &&
                (url.endsWith("mp4") ||
                        url.endsWith("avi") ||
                        url.endsWith("mov") ||
                        url.endsWith("3gp"))
            ) {
                VideoPreviewDialogFragment.show(
                    fragmentManager = supportFragmentManager,
                    uri = Uri.parse(url),
                    caption = null
                )
                return@setDownloadListener
            }
        }

        webView?.setListener(this)
    }

    private fun showRequestPermissionsAlertDialog() {
        AlertDialog.Builder(this, R.style.SOSWidgetWebViewWebView_AlertDialogTheme)
            .setCancelable(false)
            .setTitle("Доступ к разрешениям")
            .setMessage("Пожалуйста, предоставьте виджету разрешения для полноценной работы виджета")
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
        if (progress < 95) {
            progressView?.show()
            progressView?.showTextView()
            progressView?.setText("Загрузка виджета: $progress%")
        } else {
            progressView?.hide()
        }
    }

    override fun onSelectFileRequest(): Boolean {
        if (STORAGE_PERMISSIONS.all {
                ActivityCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            AlertDialog.Builder(this)
                .setTitle("Выбор медиа-вложения")
                .setItems(
                    arrayOf(
                        "Фото",
                        "Видео",
                        "Аудио",
                        "Документ"
                    )
                ) { dialog, which ->
                    dialog.dismiss()

                    when (which) {
                        0 ->
                            interactor?.launchSelection(GetContentResultContract.Params(MimeType.IMAGE))
                        1 ->
                            interactor?.launchSelection(GetContentResultContract.Params(MimeType.VIDEO))
                        2 ->
                            interactor?.launchSelection(GetContentResultContract.Params(MimeType.AUDIO))
                        3 ->
                            interactor?.launchSelection(GetContentResultContract.Params(MimeType.DOCUMENT))
                    }
                }
                .show()
        } else {
            storagePermissionsLauncher.launch(STORAGE_PERMISSIONS)
        }

        return true
    }

    override fun onPermissionRequest(resources: Array<String>) {
        val permissions = PermissionRequestMapper.fromWebClientToAndroid(resources).toTypedArray()
        Log.d(TAG, "onPermissionRequest() -> resources: ${resources.contentToString()}")
        Log.d(TAG, "onPermissionRequest() -> permissions: ${permissions.contentToString()}")
        requestedPermissionsLauncher.launch(permissions)
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