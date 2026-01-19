package game.idaima.sgcat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 创建 DataStore 实例
private val Context.floatingWindowDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "floating_window_preferences"
)

/**
 * 悬浮窗偏好数据类
 */
data class FloatingWindowData(
    val collapsedOffsetX: Float,
    val collapsedOffsetY: Float,
    val expandedOffsetX: Float,
    val expandedOffsetY: Float,
    val expandedWidth: Float,
    val expandedHeight: Float
)

/**
 * 悬浮窗状态持久化存储管理类
 */
class FloatingWindowPreferences(private val context: Context) {

    companion object {
        // 缩小模式位置
        private val COLLAPSED_OFFSET_X = floatPreferencesKey("collapsed_offset_x")
        private val COLLAPSED_OFFSET_Y = floatPreferencesKey("collapsed_offset_y")

        // 展开模式位置
        private val EXPANDED_OFFSET_X = floatPreferencesKey("expanded_offset_x")
        private val EXPANDED_OFFSET_Y = floatPreferencesKey("expanded_offset_y")

        // 展开模式尺寸（以 dp 值存储）
        private val EXPANDED_WIDTH = floatPreferencesKey("expanded_width")
        private val EXPANDED_HEIGHT = floatPreferencesKey("expanded_height")

        // 默认值
        const val DEFAULT_COLLAPSED_X = -1f // -1 表示使用计算的默认值
        const val DEFAULT_COLLAPSED_Y = 300f
        const val DEFAULT_EXPANDED_WIDTH = 320f
        const val DEFAULT_EXPANDED_HEIGHT = 480f
    }

    /**
     * 读取所有悬浮窗偏好数据
     */
    val floatingWindowData: Flow<FloatingWindowData> =
        context.floatingWindowDataStore.data.map { preferences ->
            FloatingWindowData(
                collapsedOffsetX = preferences[COLLAPSED_OFFSET_X] ?: DEFAULT_COLLAPSED_X,
                collapsedOffsetY = preferences[COLLAPSED_OFFSET_Y] ?: DEFAULT_COLLAPSED_Y,
                expandedOffsetX = preferences[EXPANDED_OFFSET_X] ?: -1f,
                expandedOffsetY = preferences[EXPANDED_OFFSET_Y] ?: -1f,
                expandedWidth = preferences[EXPANDED_WIDTH] ?: DEFAULT_EXPANDED_WIDTH,
                expandedHeight = preferences[EXPANDED_HEIGHT] ?: DEFAULT_EXPANDED_HEIGHT
            )
        }

    /**
     * 保存缩小模式位置
     */
    suspend fun saveCollapsedPosition(x: Float, y: Float) {
        context.floatingWindowDataStore.edit { preferences ->
            preferences[COLLAPSED_OFFSET_X] = x
            preferences[COLLAPSED_OFFSET_Y] = y
        }
    }

    /**
     * 保存展开模式位置
     */
    suspend fun saveExpandedPosition(x: Float, y: Float) {
        context.floatingWindowDataStore.edit { preferences ->
            preferences[EXPANDED_OFFSET_X] = x
            preferences[EXPANDED_OFFSET_Y] = y
        }
    }

    /**
     * 保存展开模式尺寸
     */
    suspend fun saveExpandedSize(width: Float, height: Float) {
        context.floatingWindowDataStore.edit { preferences ->
            preferences[EXPANDED_WIDTH] = width
            preferences[EXPANDED_HEIGHT] = height
        }
    }

    /**
     * 保存所有数据
     */
    suspend fun saveAll(data: FloatingWindowData) {
        context.floatingWindowDataStore.edit { preferences ->
            preferences[COLLAPSED_OFFSET_X] = data.collapsedOffsetX
            preferences[COLLAPSED_OFFSET_Y] = data.collapsedOffsetY
            preferences[EXPANDED_OFFSET_X] = data.expandedOffsetX
            preferences[EXPANDED_OFFSET_Y] = data.expandedOffsetY
            preferences[EXPANDED_WIDTH] = data.expandedWidth
            preferences[EXPANDED_HEIGHT] = data.expandedHeight
        }
    }
}
