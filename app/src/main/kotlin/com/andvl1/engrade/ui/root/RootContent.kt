package com.andvl1.engrade.ui.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andvl1.engrade.ui.bout.BoutScreen
import com.andvl1.engrade.ui.group.boutconfirm.BoutConfirmScreen
import com.andvl1.engrade.ui.group.boutresult.BoutResultScreen
import com.andvl1.engrade.ui.group.boutslist.BoutsListScreen
import com.andvl1.engrade.ui.group.dashboard.GroupDashboardScreen
import com.andvl1.engrade.ui.group.setup.GroupSetupScreen
import com.andvl1.engrade.ui.home.HomeScreen
import com.andvl1.engrade.ui.settings.SettingsScreen
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@Composable
fun RootContent(component: RootComponent) {
    val childStack = component.childStack.subscribeAsState()

    Children(
        stack = childStack.value,
        modifier = Modifier.fillMaxSize(),
        animation = stackAnimation(fade())
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Home -> HomeScreen(instance.component)
            is RootComponent.Child.Bout -> BoutScreen(instance.component)
            is RootComponent.Child.Settings -> SettingsScreen(instance.component)
            is RootComponent.Child.GroupSetup -> GroupSetupScreen(instance.component)
            is RootComponent.Child.GroupDashboard -> GroupDashboardScreen(instance.component)
            is RootComponent.Child.BoutConfirm -> BoutConfirmScreen(instance.component)
            is RootComponent.Child.BoutResult -> BoutResultScreen(instance.component)
            is RootComponent.Child.BoutsList -> BoutsListScreen(instance.component)
        }
    }
}
