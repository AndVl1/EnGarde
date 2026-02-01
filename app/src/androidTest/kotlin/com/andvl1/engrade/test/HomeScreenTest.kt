package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.andvl1.engrade.page.SettingsPage
import com.atiurin.ultron.extensions.*
import org.junit.Test

class HomeScreenTest : BaseTest() {

    @Test
    fun homeScreen_displaysAllButtons() {
        HomePage {
            singleBoutButton.assertIsDisplayed()
            groupStageButton.assertIsDisplayed()
            settingsButton.assertIsDisplayed()
        }
    }

    @Test
    fun homeScreen_navigateToSingleBout() {
        HomePage {
            singleBoutButton.click()
        }
        BoutPage {
            timerBox.assertIsDisplayed()
            leftScoreButton.assertIsDisplayed()
            rightScoreButton.assertIsDisplayed()
        }
    }

    @Test
    fun homeScreen_navigateToGroupStage() {
        HomePage {
            groupStageButton.click()
        }
        GroupSetupPage {
            createButton.assertExists()
        }
    }

    @Test
    fun homeScreen_navigateToSettings() {
        HomePage {
            settingsButton.click()
        }
        SettingsPage {
            sabreChip.assertIsDisplayed()
            foilEpeeChip.assertIsDisplayed()
        }
    }
}
