package com.andvl1.engrade.e2e

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.HomePage
import com.andvl1.engrade.page.SettingsPage
import com.atiurin.ultron.extensions.*
import org.junit.Test

class SingleBoutE2ETest : BaseTest() {

    @Test
    fun fullBoutCycle_leftWins() {
        // Start from home
        HomePage {
            singleBoutButton.click()
        }

        // Score 5 touches for left fencer
        BoutPage {
            leftScore.withUseUnmergedTree(true).assertTextContains("0")
            rightScore.withUseUnmergedTree(true).assertTextContains("0")

            repeat(5) {
                leftScoreButton.click()
            }

            leftScore.withUseUnmergedTree(true).assertTextContains("5")
            leftWinner.withUseUnmergedTree(true).assertIsDisplayed()
        }
    }

    @Test
    fun fullBoutCycle_rightWins() {
        HomePage {
            singleBoutButton.click()
        }

        BoutPage {
            repeat(5) {
                rightScoreButton.click()
            }

            rightScore.withUseUnmergedTree(true).assertTextContains("5")
            rightWinner.withUseUnmergedTree(true).assertIsDisplayed()
        }
    }

    @Test
    fun settingsAndBack_navigation() {
        // Home → Settings
        HomePage {
            settingsButton.click()
        }

        // Switch weapon
        SettingsPage {
            foilEpeeChip.click()
            backButton.click()
        }

        // Back to Home
        HomePage {
            singleBoutButton.assertIsDisplayed()
        }
    }
}
