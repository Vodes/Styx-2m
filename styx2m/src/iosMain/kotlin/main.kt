import androidx.compose.ui.window.ComposeUIViewController
import com.multiplatform.lifecycle.LifecycleTracker
import com.multiplatform.lifecyle.LifecycleComposeUIVCDelegate
import moe.styx.styx2m.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController(
    {
        delegate = LifecycleComposeUIVCDelegate(LifecycleTracker)
    }
) {
    App()
}