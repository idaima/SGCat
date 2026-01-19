package game.idaima.sgcat.ui

import android.annotation.SuppressLint
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import game.idaima.sgcat.util.OfflineResourceInterceptor

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
) {
    // 在 WebView 销毁时保存 Cookie
    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        AndroidView(
            factory = { context ->
                // 配置 CookieManager - 启用 Cookie 持久化
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                // 创建离线资源拦截器
                val offlineInterceptor = OfflineResourceInterceptor(context)

                WebView(context).apply {
                    // 为当前 WebView 启用第三方 Cookie
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true
                        allowContentAccess = true
                        @Suppress("DEPRECATION")
                        databaseEnabled = true
                        @Suppress("DEPRECATION")
                        savePassword = true
                        @Suppress("DEPRECATION")
                        saveFormData = true
                    }

                    // 启用自动填充支持（API 26+）
                    importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_YES

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            // 尝试使用离线资源
                            request?.let {
                                val response = offlineInterceptor.shouldInterceptRequest(it)
                                if (response != null) {
                                    return response
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        @SuppressLint("WebViewClientOnReceivedSslError")
                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            super.onReceivedSslError(view, handler, error)
                            handler?.proceed()
                        }
                    }

                    webChromeClient = WebChromeClient()

                    loadUrl(url)
                    onWebViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
