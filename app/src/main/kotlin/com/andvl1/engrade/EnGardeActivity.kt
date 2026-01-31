package com.andvl1.engrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.andvl1.engrade.data.SettingsRepository
import com.andvl1.engrade.ui.root.DefaultRootComponent
import com.andvl1.engrade.ui.root.RootContent
import com.andvl1.engrade.ui.theme.EnGardeTheme
import com.arkivanov.decompose.defaultComponentContext

class EnGardeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create dependencies manually (no DI framework)
        val settingsRepository = SettingsRepository(applicationContext)

        // Create root component
        val rootComponent = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            settingsRepository = settingsRepository
        )

        setContent {
            val useBlackBackground by settingsRepository.useBlackBackground.collectAsState(initial = false)

            EnGardeTheme(useBlackBackground = useBlackBackground) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootContent(component = rootComponent)
                }
            }
        }
    }
}
