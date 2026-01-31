package com.andvl1.engrade.ui.root

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andvl1.engrade.ui.bout.BoutScreen
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
            is RootComponent.Child.Bout -> BoutScreen(instance.component)
            is RootComponent.Child.Settings -> SettingsScreen(instance.component)
        }
    }
}
