package game.idaima.sgcat.ui

import android.annotation.SuppressLint
import android.net.http.SslError
import android.os.Bundle
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import game.idaima.sgcat.util.OfflineResourceInterceptor

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
    savedState: Bundle? = null,
    onWebViewCreated: (WebView) -> Unit = {},
    onSaveState: (WebView) -> Unit = {},
) {
    val context = LocalContext.current
    
    // 创建离线资源拦截器 - 使用 remember 避免重复创建
    val offlineInterceptor = remember { OfflineResourceInterceptor(context) }
    
    // 使用 remember 保持 WebView 实例，避免重组时重新创建
    val webView = remember {
        // 配置 CookieManager - 启用 Cookie 持久化
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

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

            // 尝试恢复状态，如果没有保存的状态则加载 URL
            if (savedState != null) {
                restoreState(savedState)
            } else {
                loadUrl(url)
            }
            
            onWebViewCreated(this)
        }
    }

    // 在 WebView 销毁时保存 Cookie 和状态
    DisposableEffect(webView) {
        onDispose {
            CookieManager.getInstance().flush()
            onSaveState(webView)
            // 清理 WebView 资源
            webView.stopLoading()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
