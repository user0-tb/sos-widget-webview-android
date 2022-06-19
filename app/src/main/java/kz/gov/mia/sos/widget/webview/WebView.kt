@file:Suppress("DEPRECATION", "SetJavaScriptEnabled")

package kz.gov.mia.sos.widget.webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.*

internal class WebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : android.webkit.WebView(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private val TAG = WebView::class.java.simpleName
    }

    data class GeolocationPermissionsShowPrompt constructor(
        val origin: String?,
        val callback: GeolocationPermissions.Callback?
    )

    data class FileSelectionPrompt constructor(
        val fileChooserParams: WebChromeClient.FileChooserParams?,
        val filePathCallback: ValueCallback<Array<Uri>>?
    )

    private var urlListener: UrlListener? = null
    private var listener: Listener? = null

    private var permissionRequest: PermissionRequest? = null
    private var geolocationPermissionsShowPrompt: GeolocationPermissionsShowPrompt? = null
    private var fileSelectionPrompt: FileSelectionPrompt? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        overScrollMode = OVER_SCROLL_ALWAYS

        isSaveEnabled = true

        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowFileAccess = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.blockNetworkImage = false
        settings.blockNetworkLoads = false
        settings.builtInZoomControls = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.databaseEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.javaScriptEnabled = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        settings.loadsImagesAutomatically = true
        settings.loadWithOverviewMode = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setSupportMultipleWindows(false)
        settings.setSupportZoom(false)
        settings.useWideViewPort = true

        // Enable remote debugging via chrome://inspect
        setWebContentsDebuggingEnabled(true)
    }

    override fun onDetachedFromWindow() {
        urlListener = null
        listener = null

        permissionRequest = null
        geolocationPermissionsShowPrompt = null
        fileSelectionPrompt = null

        destroy()

        super.onDetachedFromWindow()
    }

    fun setUrlListener(urlListener: UrlListener) {
        this.urlListener = urlListener
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun init() {
        webChromeClient = MyWebChromeClient()
        webViewClient = MyWebViewClient()
    }

    fun setupCookieManager() {
        with(CookieManager.getInstance()) {
            Log.d(TAG, "CookieManager#hasCookies(): ${hasCookies()}")

            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@WebView, true)
            flush()
        }
    }

    fun setMixedContentAllowed(allowed: Boolean) {
        settings.mixedContentMode = if (allowed) {
            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        } else {
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
    }

    fun setPermissionRequestResult(permissions: List<String>) {
        Log.d(TAG, "setPermissionRequestResult() -> permissions: ${permissions.joinToString(", ")}")
        if (permissions.isEmpty()) {
            permissionRequest?.deny()
        } else {
            Log.d(TAG, "setPermissionRequestResult() -> $permissionRequest grant permissions: ${permissions.joinToString(", ")}")
            permissionRequest?.grant(permissions.toTypedArray())
        }
        permissionRequest = null
    }

    fun setGeolocationPermissionsShowPromptResult(success: Boolean) {
        Log.d(
            TAG,
            "setGeolocationPermissionsShowPromptResult() -> $success, $geolocationPermissionsShowPrompt"
        )
        if (success) {
            geolocationPermissionsShowPrompt?.callback?.invoke(
                geolocationPermissionsShowPrompt?.origin,
                true,
                false
            )
        } else {
            geolocationPermissionsShowPrompt?.callback?.invoke(
                geolocationPermissionsShowPrompt?.origin,
                false,
                false
            )
        }
        geolocationPermissionsShowPrompt = null
    }

    fun setFileSelectionPromptResult(uri: Uri?) {
        if (uri == null) {
            setFileSelectionPromptResult(arrayOf())
        } else {
            setFileSelectionPromptResult(arrayOf(uri))
        }
    }

    fun setFileSelectionPromptResult(uris: Array<Uri>?) {
        Log.d(
            TAG,
            "setFileSelectionPromptResult() -> $uris, $fileSelectionPrompt"
        )

        try {
            if (uris.isNullOrEmpty()) {
                fileSelectionPrompt?.filePathCallback?.onReceiveValue(null)
            } else {
                fileSelectionPrompt?.filePathCallback?.onReceiveValue(uris)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fileSelectionPrompt = null
    }

    private inner class MyWebChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            Log.d(
                TAG,
                "onPermissionRequest() -> " +
                        "origin: ${request?.origin}, " +
                        "resources: ${request?.resources?.contentToString()}"
            )

            permissionRequest = request

            if (request != null) {
                listener?.onPermissionRequest(request.resources)
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            super.onPermissionRequestCanceled(request)

            Log.d(TAG, "onPermissionRequestCanceled() -> request: $request")

            permissionRequest = null

            if (request != null) {
                listener?.onPermissionRequestCanceled(request.resources)
            }
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            super.onGeolocationPermissionsShowPrompt(origin, callback)

            Log.d(
                TAG,
                "onGeolocationPermissionsShowPrompt() -> origin: $origin, callback: $callback"
            )

            geolocationPermissionsShowPrompt = GeolocationPermissionsShowPrompt(
                origin = origin,
                callback = callback
            )

            listener?.onGeolocationPermissionsShowPrompt()
        }

        override fun onGeolocationPermissionsHidePrompt() {
            super.onGeolocationPermissionsHidePrompt()

            Log.d(TAG, "onGeolocationPermissionsHidePrompt()")

            geolocationPermissionsShowPrompt = null

            listener?.onGeolocationPermissionsHidePrompt()
        }

        override fun onJsAlert(
            view: android.webkit.WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            Log.d(
                TAG,
                "onJsAlert() -> " +
                        "url: $url, " +
                        "message: $message, " +
                        "result: $result"
            )
            return super.onJsAlert(view, url, message, result)
        }

        override fun onJsConfirm(
            view: android.webkit.WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            Log.d(
                TAG,
                "onJsConfirm() -> " +
                        "url: $url, " +
                        "message: $message, " +
                        "result: $result"
            )
            return super.onJsConfirm(view, url, message, result)
        }

        override fun onJsPrompt(
            view: android.webkit.WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            Log.d(
                TAG,
                "onJsPrompt() -> " +
                        "url: $url, " +
                        "message: $message, " +
                        "defaultValue: $defaultValue, " +
                        "result: $result"
            )
            return super.onJsPrompt(view, url, message, defaultValue, result)
        }

        override fun onCreateWindow(
            view: android.webkit.WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }

        override fun onShowFileChooser(
            webView: android.webkit.WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            Log.d(
                TAG,
                "onShowFileChooser() -> params: " +
                        "${fileChooserParams?.acceptTypes?.contentToString()}, " +
                        "${fileChooserParams?.filenameHint}, " +
                        "${fileChooserParams?.isCaptureEnabled}, " +
                        "${fileChooserParams?.mode}, " +
                        "${fileChooserParams?.title}," +
                        "${fileChooserParams?.createIntent()}"
            )

            fileSelectionPrompt = FileSelectionPrompt(
                fileChooserParams = fileChooserParams,
                filePathCallback = filePathCallback
            )

            return listener?.onSelectFileRequest() == true
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)
            Log.d(TAG, "onShowCustomView()")
        }

        override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            listener?.onPageLoadProgress(newProgress)
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            return if (BuildConfig.DEBUG) {
                Log.d(TAG, consoleMessage?.message() ?: "Client received console message")
                super.onConsoleMessage(consoleMessage)
            } else {
                false
            }
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun onLoadResource(view: android.webkit.WebView?, url: String?) {
            super.onLoadResource(view, url)
            Log.d(TAG, "onLoadResource() -> url: $url")
        }

        override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d(TAG, "onPageStarted() -> url: $url")
        }

        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "onPageFinished() -> url: $url")
        }

        override fun shouldOverrideUrlLoading(
            view: android.webkit.WebView?,
            request: WebResourceRequest?
        ): Boolean {
            Log.d(TAG, "shouldOverrideUrlLoading() -> ${request?.requestHeaders}, ${request?.url}")

            return if (request == null) {
                false
            } else {
                if (urlListener?.onLoadUrl(request.requestHeaders, request.url) == true) {
                    true
                } else {
                    super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        override fun shouldOverrideUrlLoading(
            view: android.webkit.WebView?,
            url: String?
        ): Boolean {
            Log.d(TAG, "shouldOverrideUrlLoading() -> url: $url")
            return super.shouldOverrideUrlLoading(view, url)
        }

        override fun onReceivedError(
            view: android.webkit.WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            Log.d(TAG, "onReceivedError() -> $request, $error")
        }

        override fun onReceivedHttpError(
            view: android.webkit.WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            Log.d(TAG, "onReceivedHttpError() -> $request, $errorResponse")
        }

        override fun onReceivedSslError(
            view: android.webkit.WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            Log.d(TAG, "onReceivedSslError() -> $handler, $error")
            listener?.onReceivedSSLError(handler, error)
        }
    }

    fun interface UrlListener {
        fun onLoadUrl(headers: Map<String, String>?, uri: Uri): Boolean
    }

    interface Listener {
        fun onReceivedSSLError(handler: SslErrorHandler?, error: SslError?)
        fun onPageLoadProgress(progress: Int)
        fun onSelectFileRequest(): Boolean
        fun onPermissionRequest(resources: Array<String>)
        fun onPermissionRequestCanceled(resources: Array<String>)
        fun onGeolocationPermissionsShowPrompt()
        fun onGeolocationPermissionsHidePrompt()
    }

}
