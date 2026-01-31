package com.andvl1.engrade

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.data.SettingsRepository
import com.andvl1.engrade.data.db.EnGardeDatabase
import com.andvl1.engrade.domain.PoolEngine
import com.andvl1.engrade.platform.NotificationHelper
import com.andvl1.engrade.platform.SoundManager
import com.andvl1.engrade.ui.root.DefaultRootComponent
import com.andvl1.engrade.ui.root.RootContent
import com.andvl1.engrade.ui.theme.EnGardeTheme
import com.arkivanov.decompose.defaultComponentContext

class EnGardeActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room database
        val database = Room.databaseBuilder(
            applicationContext,
            EnGardeDatabase::class.java,
            "engarde.db"
        ).build()

        // Create dependencies manually (no DI framework)
        val settingsRepository = SettingsRepository(applicationContext)
        val poolRepository = PoolRepository(database)
        val poolEngine = PoolEngine()
        soundManager = SoundManager(applicationContext)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, EnGardeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationHelper = NotificationHelper(applicationContext)

        // Create root component
        val rootComponent = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            settingsRepository = settingsRepository,
            poolRepository = poolRepository,
            poolEngine = poolEngine,
            soundManager = soundManager,
            notificationHelper = notificationHelper,
            notificationPendingIntent = pendingIntent
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

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
