package moe.styx.styx2m

import Styx2m.styx2m.BuildConfig
import kotlinx.cinterop.ExperimentalForeignApi
import moe.styx.common.compose.AppConfig
import moe.styx.common.compose.AppContextImpl.appConfig
import moe.styx.common.http.getHttpClient
import moe.styx.common.util.Log
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

fun setupIosApp() {
    getHttpClient("${BuildConfig.APP_NAME} (iOS) - ${BuildConfig.APP_VERSION}")
    appConfig = { deviceAppConfig }
    Log.debugEnabled = true
}

private val deviceAppConfig: AppConfig by lazy { fetchDeviceAppConfig() }

@OptIn(ExperimentalForeignApi::class)
fun fetchDeviceAppConfig(): AppConfig {
    val appSupportPath = iosDirectory(NSApplicationSupportDirectory) + "/Styx2m"
    val cachePath = iosDirectory(NSCachesDirectory) + "/Styx2m"

    NSFileManager.defaultManager.createDirectoryAtPath(
        path = appSupportPath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = cachePath,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )

    return AppConfig(
        BuildConfig.APP_SECRET,
        BuildConfig.APP_VERSION,
        BuildConfig.BASE_URL,
        BuildConfig.IMAGE_URL,
        BuildConfig.DEBUG_TOKEN,
        cachePath,
        appSupportPath,
        BuildConfig.VERSION_CHECK_URL,
        BuildConfig.SITE,
        BuildConfig.SITE_URL
    )
}

private fun iosDirectory(directory: ULong): String {
    return NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
        .firstOrNull() as? String
        ?: error("Could not resolve iOS sandbox directory: $directory")
}
