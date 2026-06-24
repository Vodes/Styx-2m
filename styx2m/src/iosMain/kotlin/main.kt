import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ComposeUIViewController
import com.multiplatform.lifecycle.LifecycleTracker
import com.multiplatform.lifecyle.LifecycleComposeUIVCDelegate
import moe.styx.styx2m.App
import moe.styx.styx2m.setupIosApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    setupIosApp()
    return ComposeUIViewController(
        {
            delegate = LifecycleComposeUIVCDelegate(LifecycleTracker)
        }
    ) {
        val baseDensity = LocalDensity.current
        val newDensity = Density(baseDensity.density * 0.975F, baseDensity.fontScale * 0.99F)
        CompositionLocalProvider(
            LocalDensity provides newDensity
        ) {
            App()
        }
    }
}
