package kattcrazy.calendar2alarm

import android.content.Context
import android.content.pm.PackageManager

enum class ViewerApp(val key: String, val packageName: String, val labelRes: Int) {
    GOOGLE("google", "com.google.android.calendar", R.string.viewer_google),
    SAMSUNG("samsung", "com.samsung.android.calendar", R.string.viewer_samsung),
    ;

    companion object {
        fun fromKey(key: String?): ViewerApp =
            entries.firstOrNull { it.key == key } ?: GOOGLE
    }
}

fun ViewerApp.isInstalled(context: Context): Boolean =
    try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
