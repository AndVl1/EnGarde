package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.andvl1.engrade.page.SettingsPage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

@Epic("Navigation")
@Feature("Home Screen")
class HomeScreenTest : BaseTest() {

    @Test
    fun homeScreen_displaysAllButtons() {
        step("Verify all navigation buttons are displayed on home screen") {
            HomePage {
                singleBoutButton.assertIsDisplayed()
                groupStageButton.assertIsDisplayed()
                settingsButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun homeScreen_navigateToSingleBout() {
        step("Click single bout button") {
            HomePage {
                singleBoutButton.click()
            }
        }
        step("Verify bout screen is displayed") {
            BoutPage {
                timerBox.assertIsDisplayed()
                leftScoreButton.assertIsDisplayed()
                rightScoreButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun homeScreen_navigateToGroupStage() {
        step("Click group stage button") {
            HomePage {
                groupStageButton.click()
            }
        }
        step("Verify group setup screen is displayed") {
            GroupSetupPage {
                createButton.assertExists()
            }
        }
    }

    @Test
    fun homeScreen_navigateToSettings() {
        step("Click settings button") {
            HomePage {
                settingsButton.click()
            }
        }
        step("Verify settings screen is displayed") {
            SettingsPage {
                sabreChip.assertIsDisplayed()
                foilEpeeChip.assertIsDisplayed()
            }
        }
    }
}
