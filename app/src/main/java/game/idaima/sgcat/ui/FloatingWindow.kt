package game.idaima.sgcat.ui

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import game.idaima.sgcat.data.FloatingWindowPreferences
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 应用内悬浮窗组件
 * 支持拖拽、缩小/展开、边缘吸附、双指缩放
 * 状态持久化到 DataStore
 * WebView 在模式切换时保持不被销毁
 */
@SuppressLint("ConfigurationScreenWidthHeight", "SetJavaScriptEnabled")
@Composable
fun FloatingWindow(
    modifier: Modifier = Modifier,
    url: String = "http://81.69.17.107:81/gm",
    mainWebView: WebView? = null,
    onResetMainPage: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // DataStore 偏好管理
    val preferences = remember { FloatingWindowPreferences(context) }

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 悬浮窗状态
    var isExpanded by remember { mutableStateOf(false) }
    var floatingWebView by remember { mutableStateOf<WebView?>(null) }

    // 缩小状态的按钮尺寸
    val collapsedSize = 56.dp
    val collapsedSizePx = with(density) { collapsedSize.toPx() }

    // 展开状态的窗口尺寸（可动态调整）
    val minWidth = 200.dp
    val minHeight = 300.dp
    val maxWidth = configuration.screenWidthDp.dp - 32.dp
    val maxHeight = configuration.screenHeightDp.dp - 100.dp

    // 展开模式尺寸
    var expandedWidth by remember { mutableStateOf(320.dp) }
    var expandedHeight by remember { mutableStateOf(480.dp) }

    // 缩小状态的坐标（独立记录）
    var collapsedOffsetX by remember { mutableFloatStateOf(screenWidthPx - 200f) }
    var collapsedOffsetY by remember { mutableFloatStateOf(300f) }

    // 展开状态的坐标（独立记录）
    var expandedOffsetX by remember {
        mutableFloatStateOf((screenWidthPx - with(density) { expandedWidth.toPx() }) / 2)
    }
    var expandedOffsetY by remember {
        mutableFloatStateOf((screenHeightPx - with(density) { expandedHeight.toPx() }) / 2)
    }

    // 从 DataStore 加载保存的数据（只执行一次）
    LaunchedEffect(Unit) {
        // 获取第一个非空的保存数据
        preferences.floatingWindowData.collect { data ->
            // 恢复缩小模式位置
            collapsedOffsetX = if (data.collapsedOffsetX < 0) {
                screenWidthPx - 200f
            } else {
                data.collapsedOffsetX
            }
            collapsedOffsetY = data.collapsedOffsetY

            // 恢复展开模式位置
            val savedExpandedWidth = data.expandedWidth.dp
            val savedExpandedHeight = data.expandedHeight.dp
            expandedWidth = savedExpandedWidth.coerceIn(minWidth, maxWidth)
            expandedHeight = savedExpandedHeight.coerceIn(minHeight, maxHeight)

            val expandedWidthPx = with(density) { expandedWidth.toPx() }
            val expandedHeightPx = with(density) { expandedHeight.toPx() }

            expandedOffsetX = if (data.expandedOffsetX < 0) {
                (screenWidthPx - expandedWidthPx) / 2
            } else {
                data.expandedOffsetX.coerceIn(0f, screenWidthPx - expandedWidthPx)
            }
            expandedOffsetY = if (data.expandedOffsetY < 0) {
                (screenHeightPx - expandedHeightPx) / 2
            } else {
                data.expandedOffsetY.coerceIn(0f, screenHeightPx - expandedHeightPx)
            }

            // 只需要第一次加载，之后退出收集
            return@collect
        }
    }

    // 保存缩小模式位置的函数
    fun saveCollapsedPosition() {
        scope.launch {
            preferences.saveCollapsedPosition(collapsedOffsetX, collapsedOffsetY)
        }
    }

    // 保存展开模式位置的函数
    fun saveExpandedPosition() {
        scope.launch {
            preferences.saveExpandedPosition(expandedOffsetX, expandedOffsetY)
        }
    }

    // 保存展开模式尺寸的函数
    fun saveExpandedSize() {
        scope.launch {
            preferences.saveExpandedSize(expandedWidth.value, expandedHeight.value)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 缩小状态：悬浮按钮
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            collapsedOffsetX.roundToInt(),
                            collapsedOffsetY.roundToInt()
                        )
                    }
                    .size(collapsedSize)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // 边缘吸附：拖拽结束后吸附到最近的边缘
                                val centerX = collapsedOffsetX + collapsedSizePx / 2
                                collapsedOffsetX = if (centerX < screenWidthPx / 2) {
                                    0f // 吸附到左边
                                } else {
                                    screenWidthPx - collapsedSizePx // 吸附到右边
                                }
                                // 限制 Y 坐标在屏幕范围内
                                collapsedOffsetY =
                                    collapsedOffsetY.coerceIn(0f, screenHeightPx - collapsedSizePx)
                                // 保存位置到 DataStore
                                saveCollapsedPosition()
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            collapsedOffsetX += dragAmount.x
                            collapsedOffsetY += dragAmount.y
                        }
                    },
                shape = CircleShape,
                color = Color(0xFF6200EE),
                shadowElevation = 8.dp,
                onClick = { isExpanded = true }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "打开 GM 面板",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 展开状态：悬浮窗容器（使用 alpha 而非 AnimatedVisibility 以保持 WebView）
        val expandedWidthPx = with(density) { expandedWidth.toPx() }
        val expandedHeightPx = with(density) { expandedHeight.toPx() }

        Box(
            modifier = Modifier
                .offset {
                    if (isExpanded) {
                        IntOffset(
                            expandedOffsetX.roundToInt(),
                            expandedOffsetY.roundToInt()
                        )
                    } else {
                        // 缩小时移到屏幕外
                        IntOffset(-10000, -10000)
                    }
                }
                .width(expandedWidth)
                .height(expandedHeight)
                .alpha(if (isExpanded) 1f else 0f)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 标题栏：可拖拽区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF6200EE))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .pointerInput(isExpanded) {
                                if (isExpanded) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            // 保存位置到 DataStore
                                            saveExpandedPosition()
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        expandedOffsetX = (expandedOffsetX + dragAmount.x)
                                            .coerceIn(0f, screenWidthPx - expandedWidthPx)
                                        expandedOffsetY = (expandedOffsetY + dragAmount.y)
                                            .coerceIn(0f, screenHeightPx - expandedHeightPx)
                                    }
                                }
                            }
                    ) {
                        // 右侧按钮组：关闭 + 更多
                        var showMenu by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 关闭按钮（进入缩小模式）
                            IconButton(onClick = { isExpanded = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "缩小",
                                    tint = Color.White
                                )
                            }

                            // 更多菜单按钮
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "更多",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("重载游戏") },
                                        onClick = {
                                            onResetMainPage()
                                            showMenu = false
                                            isExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("重载GM") },
                                        onClick = {
                                            floatingWebView?.reload()
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("清除缓存") },
                                        onClick = {
                                            // 清除 WebView 缓存
                                            mainWebView?.clearCache(true)
                                            floatingWebView?.clearCache(true)
                                            // 重新加载所有 H5 页面
                                            mainWebView?.reload()
                                            floatingWebView?.reload()
                                            showMenu = false
                                            isExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("退出登录") },
                                        onClick = {
                                            // 清除主 WebView 的登录信息并刷新页面
                                            mainWebView?.evaluateJavascript(
                                                "localStorage.removeItem('game_login_info'); location.reload();",
                                                null
                                            )
                                            showMenu = false
                                            isExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // WebView 内容 - 始终保持在组合中
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    ) {
                        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        mixedContentMode =
                                            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        setSupportZoom(true)
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                    }

                                    importantForAutofill =
                                        android.view.View.IMPORTANT_FOR_AUTOFILL_YES

                                    webViewClient = object : WebViewClient() {
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
                                    floatingWebView = this
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 右下角缩放手柄
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(
                            Color(0xFF6200EE).copy(alpha = 0.8f),
                            RoundedCornerShape(topStart = 8.dp)
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    // 保存尺寸到 DataStore
                                    saveExpandedSize()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                // 调整宽度和高度
                                val newWidth = expandedWidth + with(density) { dragAmount.x.toDp() }
                                val newHeight =
                                    expandedHeight + with(density) { dragAmount.y.toDp() }

                                expandedWidth = newWidth.coerceIn(minWidth, maxWidth)
                                expandedHeight = newHeight.coerceIn(minHeight, maxHeight)
                            }
                        }
                )
            }
        }
    }
}
