package com.andvl1.engrade.ui.root

import com.andvl1.engrade.data.SettingsRepository
import com.andvl1.engrade.ui.bout.BoutComponent
import com.andvl1.engrade.ui.bout.DefaultBoutComponent
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
        data class Bout(val component: BoutComponent) : Child()
        data class Settings(val component: SettingsComponent) : Child()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object Bout : Config()

        @Serializable
        data object Settings : Config()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val settingsRepository: SettingsRepository
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<RootComponent.Config>()

    override val childStack: Value<ChildStack<RootComponent.Config, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = RootComponent.Config.serializer(),
            initialConfiguration = RootComponent.Config.Bout,
            handleBackButton = true,
            childFactory = ::createChild
        )

    private fun createChild(
        config: RootComponent.Config,
        context: ComponentContext
    ): RootComponent.Child = when (config) {
        RootComponent.Config.Bout -> RootComponent.Child.Bout(
            DefaultBoutComponent(
                componentContext = context,
                settingsRepository = settingsRepository,
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
    }

    override fun navigateToSettings() {
        navigation.push(RootComponent.Config.Settings)
    }

    override fun navigateBack() {
        navigation.pop()
    }
}
