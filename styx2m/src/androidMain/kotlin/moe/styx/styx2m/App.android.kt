package moe.styx.styx2m

import Styx_m.styx_m.BuildConfig
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.multiplatform.lifecycle.LifecycleTracker
import com.multiplatform.lifecyle.AndroidLifecycleEventObserver
import moe.styx.common.compose.AppConfig
import moe.styx.common.compose.AppContextImpl
import moe.styx.common.compose.AppContextImpl.appConfig
import moe.styx.common.http.getHttpClient
import moe.styx.common.util.Log

class AndroidApp : Application() {
    companion object {
        lateinit var INSTANCE: AndroidApp
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        AppContextImpl.setUp(this)
    }
}

class AppActivity : ComponentActivity() {
    private val observer = AndroidLifecycleEventObserver(LifecycleTracker)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        enableEdgeToEdge()
        lifecycle.addObserver(observer)
        getHttpClient("${BuildConfig.APP_NAME} (Android) - ${BuildConfig.APP_VERSION}")
        appConfig = { fetchDeviceAppConfig(this) }
        Log.debugEnabled = true
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(observer)
    }
}

fun fetchDeviceAppConfig(context: Context?): AppConfig {
    val ensuredContext = context ?: AppContextImpl.get()
    return AppConfig(
        BuildConfig.APP_SECRET,
        BuildConfig.APP_VERSION,
        BuildConfig.BASE_URL,
        BuildConfig.IMAGE_URL,
        BuildConfig.DEBUG_TOKEN,
        ensuredContext.cacheDir.path,
        ensuredContext.filesDir.path,
        BuildConfig.VERSION_CHECK_URL
    )
}

internal actual fun openUrl(url: String?) {
    val uri = url?.let { Uri.parse(it) } ?: return
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = uri
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    AndroidApp.INSTANCE.startActivity(intent)
}