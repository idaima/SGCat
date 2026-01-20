package game.idaima.sgcat

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import game.idaima.sgcat.ui.FloatingWindow
import game.idaima.sgcat.ui.WebViewScreen
import game.idaima.sgcat.ui.theme.SGCatTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val GAME_URL = "http://81.69.17.107:81"
        private const val GM_URL = "http://81.69.17.107:81/gm"
        private const val KEY_WEBVIEW_STATE = "webview_state"
    }

    // 保存 WebView 状态的 Bundle
    private var savedWebViewState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 恢复保存的 WebView 状态
        savedWebViewState = savedInstanceState?.getBundle(KEY_WEBVIEW_STATE)
        
        enableEdgeToEdge()
        hideSystemBars()

        // 禁用返回手势
        onBackPressedDispatcher.addCallback(this) {
            // 不执行任何操作，禁用返回
        }

        setContent {
            SGCatTheme {
                // 主 WebView 引用
                var mainWebView by remember { mutableStateOf<WebView?>(null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 主 WebView
                        WebViewScreen(
                            url = GAME_URL,
                            savedState = savedWebViewState,
                            onWebViewCreated = { webView ->
                                mainWebView = webView
                            },
                            onSaveState = { webView ->
                                // 保存 WebView 状态供后续使用
                                val bundle = Bundle()
                                webView.saveState(bundle)
                                savedWebViewState = bundle
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .systemBarsPadding()
                        )

                        // 悬浮窗
                        FloatingWindow(
                            url = GM_URL,
                            mainWebView = mainWebView,
                            onResetMainPage = {
                                mainWebView?.loadUrl(GAME_URL)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .systemBarsPadding()
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存 WebView 状态
        savedWebViewState?.let {
            outState.putBundle(KEY_WEBVIEW_STATE, it)
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}