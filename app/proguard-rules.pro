# ============================
# 基础优化规则
# ============================

# 启用更激进的优化
-optimizationpasses 7
-allowaccessmodification
-mergeinterfacesaggressively
-repackageclasses ''

# 移除日志代码
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 移除 Kotlin 调试信息
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# ============================
# Compose 规则
# ============================

# 保留 Compose 运行时必需的类
-keep class androidx.compose.runtime.** { *; }

# ============================
# WebView JavaScript 接口（如有需要取消注释）
# ============================
#-keepclassmembers class your.package.JavaScriptInterface {
#   public *;
#}

# ============================
# 移除 Kotlin Metadata
# ============================
-dontwarn kotlin.Metadata
-dontwarn kotlin.reflect.**
