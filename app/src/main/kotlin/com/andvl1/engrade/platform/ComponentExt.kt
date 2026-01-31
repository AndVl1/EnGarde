package com.andvl1.engrade.platform

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Creates a coroutine scope tied to the component's lifecycle.
 * Automatically cancelled when component is destroyed.
 */
fun ComponentContext.componentScope(): CoroutineScope {
    val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    lifecycle.doOnDestroy { scope.cancel() }
    return scope
}
