package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.HomePage
import com.andvl1.engrade.page.SettingsPage
import com.atiurin.ultron.extensions.*
import org.junit.Test

class SettingsScreenTest : BaseTest() {

    private fun navigateToSettings() {
        HomePage {
            settingsButton.click()
        }
    }

    @Test
    fun settingsScreen_displaysAllOptions() {
        navigateToSettings()
        SettingsPage {
            sabreChip.assertIsDisplayed()
            foilEpeeChip.assertIsDisplayed()
            mode5Chip.assertIsDisplayed()
            mode15Chip.assertIsDisplayed()
            showDoubleSwitch.assertIsDisplayed()
            anywhereToStartSwitch.assertIsDisplayed()
            blackBackgroundSwitch.assertIsDisplayed()
        }
    }

    @Test
    fun settingsScreen_switchWeapon() {
        navigateToSettings()
        SettingsPage {
            foilEpeeChip.click()
            foilEpeeChip.assertIsSelected()
        }
    }

    @Test
    fun settingsScreen_switchMode() {
        navigateToSettings()
        SettingsPage {
            mode15Chip.click()
            mode15Chip.assertIsSelected()
        }
    }

    @Test
    fun settingsScreen_toggleBlackBackground() {
        navigateToSettings()
        SettingsPage {
            blackBackgroundSwitch.click()
        }
    }

    @Test
    fun settingsScreen_navigateBack() {
        navigateToSettings()
        SettingsPage {
            backButton.click()
        }
        HomePage {
            singleBoutButton.assertIsDisplayed()
        }
    }
}
