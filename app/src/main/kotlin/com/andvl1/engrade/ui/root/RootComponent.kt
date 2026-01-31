package com.andvl1.engrade.ui.root

import android.app.PendingIntent
import com.andvl1.engrade.data.PoolRepository
import com.andvl1.engrade.data.SettingsRepository
import com.andvl1.engrade.domain.PoolEngine
import com.andvl1.engrade.platform.NotificationHelper
import com.andvl1.engrade.platform.PdfExporter
import com.andvl1.engrade.platform.SoundManager
import com.andvl1.engrade.ui.bout.BoutComponent
import com.andvl1.engrade.ui.bout.DefaultBoutComponent
import com.andvl1.engrade.ui.group.setup.GroupSetupComponent
import com.andvl1.engrade.ui.group.dashboard.GroupDashboardComponent
import com.andvl1.engrade.ui.group.boutconfirm.BoutConfirmComponent
import com.andvl1.engrade.ui.group.boutresult.BoutResultComponent
import com.andvl1.engrade.ui.group.boutslist.BoutsListComponent
import com.andvl1.engrade.ui.home.HomeComponent
import com.andvl1.engrade.ui.settings.DefaultSettingsComponent
import com.andvl1.engrade.ui.settings.SettingsComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

interface RootComponent {
    val childStack: Value<ChildStack<Config, Child>>

    fun navigateToSettings()
    fun navigateBack()

    sealed class Child {
        data class Home(val component: HomeComponent) : Child()
        data class Bout(val component: BoutComponent) : Child()
        data class Settings(val component: SettingsComponent) : Child()
        data class GroupSetup(val component: GroupSetupComponent) : Child()
        data class GroupDashboard(val component: GroupDashboardComponent) : Child()
        data class BoutConfirm(val component: BoutConfirmComponent) : Child()
        data class BoutResult(val component: BoutResultComponent) : Child()
        data class BoutsList(val component: BoutsListComponent) : Child()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object Home : Config()

        @Serializable
        data object Bout : Config()

        @Serializable
        data object Settings : Config()

        @Serializable
        data object GroupSetup : Config()

        @Serializable
        data class GroupDashboard(val poolId: Long) : Config()

        @Serializable
        data class BoutConfirm(val poolId: Long, val boutId: Long) : Config()

        @Serializable
        data class PoolBout(
            val poolId: Long,
            val boutId: Long,
            val leftName: String,
            val rightName: String,
            val mode: Int,
            val weapon: String
        ) : Config()

        @Serializable
        data class BoutResult(
            val poolId: Long,
            val boutId: Long,
            val leftName: String,
            val rightName: String,
            val leftScore: Int,
            val rightScore: Int,
            val winner: String
        ) : Config()

        @Serializable
        data class BoutsList(val poolId: Long) : Config()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository,
    private val poolRepository: PoolRepository,
    private val poolEngine: PoolEngine,
    private val pdfExporter: PdfExporter,
    private val soundManager: SoundManager,
    private val notificationHelper: NotificationHelper,
    private val notificationPendingIntent: PendingIntent
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<RootComponent.Config>()

    override val childStack: Value<ChildStack<RootComponent.Config, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = RootComponent.Config.serializer(),
            initialConfiguration = RootComponent.Config.Home,
            handleBackButton = true,
            childFactory = ::createChild
        )

    private fun createChild(
        config: RootComponent.Config,
        context: ComponentContext
    ): RootComponent.Child = when (config) {
        RootComponent.Config.Home -> RootComponent.Child.Home(
            com.andvl1.engrade.ui.home.DefaultHomeComponent(
                componentContext = context,
                poolRepository = poolRepository,
                onNavigateToSingleBout = { navigation.push(RootComponent.Config.Bout) },
                onNavigateToGroupStage = { navigation.push(RootComponent.Config.GroupSetup) },
                onNavigateToContinuePool = { poolId ->
                    navigation.push(RootComponent.Config.GroupDashboard(poolId))
                },
                onNavigateToSettings = ::navigateToSettings
            )
        )
        RootComponent.Config.Bout -> RootComponent.Child.Bout(
            DefaultBoutComponent(
                componentContext = context,
                settingsRepository = settingsRepository,
                soundManager = soundManager,
                notificationHelper = notificationHelper,
                notificationPendingIntent = notificationPendingIntent,
                onNavigateToSettings = ::navigateToSettings
            )
        )
        RootComponent.Config.Settings -> RootComponent.Child.Settings(
            DefaultSettingsComponent(
                componentContext = context,
                settingsRepository = settingsRepository,
                onBack = ::navigateBack
            )
        )
        RootComponent.Config.GroupSetup -> RootComponent.Child.GroupSetup(
            com.andvl1.engrade.ui.group.setup.DefaultGroupSetupComponent(
                componentContext = context,
                poolRepository = poolRepository,
                onPoolCreated = { poolId ->
                    navigation.push(RootComponent.Config.GroupDashboard(poolId))
                },
                onBack = ::navigateBack
            )
        )
        is RootComponent.Config.GroupDashboard -> RootComponent.Child.GroupDashboard(
            com.andvl1.engrade.ui.group.dashboard.DefaultGroupDashboardComponent(
                componentContext = context,
                poolId = config.poolId,
                poolRepository = poolRepository,
                poolEngine = poolEngine,
                pdfExporter = pdfExporter,
                onNavigateToBoutConfirm = { poolId, boutId ->
                    navigation.push(RootComponent.Config.BoutConfirm(poolId, boutId))
                },
                onNavigateToBoutsList = { poolId ->
                    navigation.push(RootComponent.Config.BoutsList(poolId))
                },
                onBack = ::navigateBack
            )
        )
        is RootComponent.Config.BoutConfirm -> RootComponent.Child.BoutConfirm(
            com.andvl1.engrade.ui.group.boutconfirm.DefaultBoutConfirmComponent(
                componentContext = context,
                poolId = config.poolId,
                boutId = config.boutId,
                poolRepository = poolRepository,
                onStartBout = { poolId, boutId, leftName, rightName, mode, weapon ->
                    navigation.push(
                        RootComponent.Config.PoolBout(
                            poolId = poolId,
                            boutId = boutId,
                            leftName = leftName,
                            rightName = rightName,
                            mode = mode,
                            weapon = weapon
                        )
                    )
                },
                onBack = ::navigateBack
            )
        )
        is RootComponent.Config.PoolBout -> RootComponent.Child.Bout(
            DefaultBoutComponent(
                componentContext = context,
                settingsRepository = settingsRepository,
                soundManager = soundManager,
                notificationHelper = notificationHelper,
                notificationPendingIntent = notificationPendingIntent,
                leftFencerName = config.leftName,
                rightFencerName = config.rightName,
                overrideConfig = com.andvl1.engrade.domain.model.BoutConfig(
                    mode = config.mode,
                    weapon = com.andvl1.engrade.domain.model.Weapon.valueOf(config.weapon),
                    periodLengthMs = 3 * 60 * 1000L,
                    breakLengthMs = 1 * 60 * 1000L,
                    priorityLengthMs = 1 * 60 * 1000L,
                    showDoubleTouchButton = config.weapon == "SABRE",
                    anywhereToStart = true
                ),
                onBoutFinished = { leftScore, rightScore, winner ->
                    // Record result
                    navigation.replaceCurrent(
                        RootComponent.Config.BoutResult(
                            poolId = config.poolId,
                            boutId = config.boutId,
                            leftName = config.leftName,
                            rightName = config.rightName,
                            leftScore = leftScore,
                            rightScore = rightScore,
                            winner = winner.name
                        )
                    )
                },
                onNavigateToSettings = ::navigateToSettings
            )
        )
        is RootComponent.Config.BoutResult -> RootComponent.Child.BoutResult(
            com.andvl1.engrade.ui.group.boutresult.DefaultBoutResultComponent(
                componentContext = context,
                poolId = config.poolId,
                boutId = config.boutId,
                leftName = config.leftName,
                rightName = config.rightName,
                leftScore = config.leftScore,
                rightScore = config.rightScore,
                winner = config.winner,
                poolRepository = poolRepository,
                onContinue = {
                    // Pop bout result and pool bout, return to dashboard
                    navigation.pop()
                    navigation.pop()
                }
            )
        )
        is RootComponent.Config.BoutsList -> RootComponent.Child.BoutsList(
            com.andvl1.engrade.ui.group.boutslist.DefaultBoutsListComponent(
                componentContext = context,
                poolId = config.poolId,
                poolRepository = poolRepository,
                onBack = ::navigateBack
            )
        )
    }

    override fun navigateToSettings() {
        navigation.push(RootComponent.Config.Settings)
    }

    override fun navigateBack() {
        navigation.pop()
    }
}
