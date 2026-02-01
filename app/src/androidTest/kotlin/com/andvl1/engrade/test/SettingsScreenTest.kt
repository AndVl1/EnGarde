package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.HomePage
import com.andvl1.engrade.page.SettingsPage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

@Epic("Settings")
@Feature("Settings Screen")
class SettingsScreenTest : BaseTest() {

    private fun navigateToSettings() {
        HomePage {
            settingsButton.click()
        }
    }

    @Test
    fun settingsScreen_displaysAllOptions() {
        step("Navigate to settings screen") {
            navigateToSettings()
        }
        step("Verify all settings options are displayed") {
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
    }

    @Test
    fun settingsScreen_switchWeapon() {
        step("Navigate to settings screen") {
            navigateToSettings()
        }
        step("Select foil/epee weapon") {
            SettingsPage {
                foilEpeeChip.click()
                foilEpeeChip.assertIsSelected()
            }
        }
    }

    @Test
    fun settingsScreen_switchMode() {
        step("Navigate to settings screen") {
            navigateToSettings()
        }
        step("Select mode 15") {
            SettingsPage {
                mode15Chip.click()
                mode15Chip.assertIsSelected()
            }
        }
    }

    @Test
    fun settingsScreen_toggleBlackBackground() {
        step("Navigate to settings screen") {
            navigateToSettings()
        }
        step("Toggle black background switch") {
            SettingsPage {
                blackBackgroundSwitch.click()
            }
        }
    }

    @Test
    fun settingsScreen_navigateBack() {
        step("Navigate to settings screen") {
            navigateToSettings()
        }
        step("Click back button") {
            SettingsPage {
                backButton.click()
            }
        }
        step("Verify returned to home screen") {
            HomePage {
                singleBoutButton.assertIsDisplayed()
            }
        }
    }
}
