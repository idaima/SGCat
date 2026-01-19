package game.idaima.sgcat.util

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.IOException

/**
 * H5 离线资源拦截器
 * 用于拦截 WebView 请求，将在线资源替换为本地 assets/game 目录下的离线资源
 */
class OfflineResourceInterceptor(private val context: Context) {

    companion object {
        private const val TAG = "OfflineInterceptor"
        
        // 资源基础路径
        private const val ASSETS_BASE_PATH = "game"
        
        // 需要拦截的资源文件扩展名
        private val INTERCEPTABLE_EXTENSIONS = setOf(
            // JavaScript & WebAssembly
            "js", "wasm",
            // 样式
            "css",
            // 图片
            "png", "jpg", "jpeg", "gif", "webp", "svg", "ico",
            // 音频
            "mp3", "wav", "ogg", "m4a",
            // 视频
            "mp4", "webm",
            // 字体
            "ttf", "otf", "woff", "woff2", "eot",
            // Spine 骨骼动画文件
            "sk", "skel", "atlas",
            // 动画与数据文件
            "ani", "dat", "bin",
            // 配置与文本文件
            "json", "xml", "fnt", "txt"
        )
        
        // URL 路径匹配正则表达式
        // 匹配 /js/, /libs/, /libs-es6/, /res/, /assets/, /login/, /update/ 等路径
        private val PATH_PATTERN = Regex(
            ".*/(?:js|libs|libs-es6|res|assets|login|update)/.*|" +
            ".*(?:NotoSansTC-Bold\\.ttf|skillName\\.fnt|skillName\\.png)$|" +
            ".*\\.(?:sk|ani|dat|wasm|atlas|skel|bin|fnt)$",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * 拦截 WebView 请求，尝试使用本地离线资源替换
     * @param request WebView 请求
     * @return 如果有本地资源则返回 WebResourceResponse，否则返回 null 让 WebView 继续在线加载
     */
    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        return interceptUrl(url)
    }

    /**
     * 拦截 URL 请求
     * @param url 请求的 URL
     * @return 如果有本地资源则返回 WebResourceResponse，否则返回 null
     */
    fun interceptUrl(url: String): WebResourceResponse? {
        try {
            // 检查是否应该拦截此请求
            if (!shouldIntercept(url)) {
                return null
            }

            // 从 URL 中提取资源路径
            val resourcePath = extractResourcePath(url) ?: return null

            // 尝试从 assets 加载资源
            return loadFromAssets(resourcePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error intercepting request: $url", e)
            return null
        }
    }

    /**
     * 判断是否应该拦截此 URL
     */
    private fun shouldIntercept(url: String): Boolean {
        // 检查文件扩展名
        val extension = getFileExtension(url)
        if (extension.isNullOrEmpty() || extension !in INTERCEPTABLE_EXTENSIONS) {
            return false
        }

        // 检查路径是否匹配
        return PATH_PATTERN.matches(url)
    }

    /**
     * 从 URL 中提取资源路径
     * 例如: http://example.com/game/js/main.js -> js/main.js
     */
    private fun extractResourcePath(url: String): String? {
        try {
            Log.d(TAG, "extract resource path request: $url")

            // 移除查询参数和锚点
            val cleanUrl = url.split("?")[0].split("#")[0]
            
            // 尝试多种路径匹配模式
            val patterns = listOf(
                // 匹配 /game/ 后的路径
                Regex(".*/game/(.+)$"),
                // 匹配 /js/, /libs/, /res/ 等目录后的完整路径
                Regex(".*/(js/.+)$"),
                Regex(".*/(libs/.+)$"),
                Regex(".*/(libs-es6/.+)$"),
                Regex(".*/(res/.+)$"),
                Regex(".*/(assets/.+)$"),
                Regex(".*/(login/.+)$"),
                Regex(".*/(update/.+)$"),
                // 匹配根目录下的特定文件（包括 sk、ani、dat 等）
                Regex(".*/([^/]+\\.(?:ttf|fnt|png|sk|ani|dat|wasm|atlas|skel|bin))$")
            )

            for (pattern in patterns) {
                val match = pattern.find(cleanUrl)
                if (match != null) {
                    return match.groupValues[1]
                }
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting resource path from: $url", e)
            return null
        }
    }

    /**
     * 从 assets 目录加载资源
     */
    private fun loadFromAssets(resourcePath: String): WebResourceResponse? {
        val assetPath = "$ASSETS_BASE_PATH/$resourcePath"

        return try {
            val inputStream = context.assets.open(assetPath)
            val mimeType = getMimeType(resourcePath)
            
            Log.d(TAG, "Loading offline resource: $assetPath (MIME: $mimeType)")

            WebResourceResponse(
                mimeType,
                "UTF-8",
                inputStream
            ).apply {
                // 设置响应头，允许跨域访问
                responseHeaders = mapOf(
                    "Access-Control-Allow-Origin" to "*",
                    "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
                    "Access-Control-Allow-Headers" to "*",
                    "Cache-Control" to "max-age=31536000"
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // 资源不存在于 assets 中，返回 null 让 WebView 从网络加载
            Log.d(TAG, "Resource not found in assets: $assetPath")
            null
        }
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(url: String): String? {
        val cleanUrl = url.split("?")[0].split("#")[0]
        val lastDot = cleanUrl.lastIndexOf('.')
        if (lastDot == -1 || lastDot == cleanUrl.length - 1) {
            return null
        }
        return cleanUrl.substring(lastDot + 1).lowercase()
    }

    /**
     * 根据文件扩展名获取 MIME 类型
     */
    private fun getMimeType(path: String): String {
        val extension = getFileExtension(path)?.lowercase()
        
        // 自定义 MIME 类型映射
        val customMimeTypes = mapOf(
            // JavaScript & WebAssembly
            "js" to "application/javascript",
            "wasm" to "application/wasm",
            // 配置与数据
            "json" to "application/json",
            "xml" to "application/xml",
            "txt" to "text/plain",
            // 样式
            "css" to "text/css",
            "html" to "text/html",
            "htm" to "text/html",
            // 图片
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "svg" to "image/svg+xml",
            "ico" to "image/x-icon",
            // 音频
            "mp3" to "audio/mpeg",
            "wav" to "audio/wav",
            "ogg" to "audio/ogg",
            "m4a" to "audio/mp4",
            // 视频
            "mp4" to "video/mp4",
            "webm" to "video/webm",
            // 字体
            "ttf" to "font/ttf",
            "otf" to "font/otf",
            "woff" to "font/woff",
            "woff2" to "font/woff2",
            "eot" to "application/vnd.ms-fontobject",
            // Spine 骨骼动画文件
            "sk" to "application/octet-stream",
            "skel" to "application/octet-stream",
            "atlas" to "text/plain",
            // 动画与二进制数据
            "ani" to "application/octet-stream",
            "dat" to "application/octet-stream",
            "bin" to "application/octet-stream",
            "fnt" to "application/octet-stream"
        )

        return customMimeTypes[extension]
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}
