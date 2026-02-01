package com.andvl1.engrade.base

import android.os.Environment
import com.andvl1.engrade.EnGardeActivity
import com.atiurin.ultron.allure.config.UltronAllureConfig
import com.atiurin.ultron.core.compose.config.UltronComposeConfig
import com.atiurin.ultron.core.compose.createUltronComposeRule
import com.atiurin.ultron.core.config.UltronConfig
import org.junit.BeforeClass
import org.junit.Rule

open class BaseTest {

    @get:Rule
    val composeRule = createUltronComposeRule<EnGardeActivity>()

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupUltron() {
            UltronConfig.applyRecommended()
            UltronComposeConfig.applyRecommended()
            UltronAllureConfig.applyRecommended()
            UltronAllureConfig.setAllureResultsDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
    }
}
