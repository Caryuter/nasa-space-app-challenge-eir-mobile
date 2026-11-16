package mx.edu.utez.eir_nasa_space_apps_challenge.data.auth

import android.content.Context
import java.util.UUID

class DeviceIdManager private constructor(
    private val prefs: android.content.SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("eir_device_prefs", Context.MODE_PRIVATE)
    )

    fun getOrCreateDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = "DEVICE-" + UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    fun setDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    companion object {
        private const val KEY_DEVICE_ID = "stable_device_id"

        @Volatile
        private var INSTANCE: DeviceIdManager? = null

        fun getInstance(context: Context): DeviceIdManager {
            return INSTANCE ?: synchronized(this) {
                val instance = DeviceIdManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun createForTesting(deviceId: String): DeviceIdManager {
            val dummyPrefs = object : android.content.SharedPreferences {
                private val map = mutableMapOf<String, Any?>("stable_device_id" to deviceId)
                override fun getAll(): MutableMap<String, *> = map
                override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
                override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
                override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
                override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
                override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
                override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
                override fun contains(key: String?): Boolean = map.containsKey(key)
                override fun edit(): android.content.SharedPreferences.Editor = object : android.content.SharedPreferences.Editor {
                    override fun putString(k: String?, v: String?): android.content.SharedPreferences.Editor { map[k ?: ""] = v; return this }
                    override fun putStringSet(k: String?, v: MutableSet<String>?): android.content.SharedPreferences.Editor = this
                    override fun putInt(k: String?, v: Int): android.content.SharedPreferences.Editor { map[k ?: ""] = v; return this }
                    override fun putLong(k: String?, v: Long): android.content.SharedPreferences.Editor { map[k ?: ""] = v; return this }
                    override fun putFloat(k: String?, v: Float): android.content.SharedPreferences.Editor { map[k ?: ""] = v; return this }
                    override fun putBoolean(k: String?, v: Boolean): android.content.SharedPreferences.Editor { map[k ?: ""] = v; return this }
                    override fun remove(k: String?): android.content.SharedPreferences.Editor { map.remove(k); return this }
                    override fun clear(): android.content.SharedPreferences.Editor { map.clear(); return this }
                    override fun commit(): Boolean = true
                    override fun apply() {}
                }
                override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
                override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
            }
            return DeviceIdManager(dummyPrefs)
        }
    }
}
