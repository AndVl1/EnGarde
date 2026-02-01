package com.andvl1.engrade.e2e

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.HomePage
import com.andvl1.engrade.page.SettingsPage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Allure.step
import org.junit.Test

@Epic("E2E")
@Feature("Single Bout Flow")
class SingleBoutE2ETest : BaseTest() {

    @Test
    fun fullBoutCycle_leftWins() {
        step("Start from home and navigate to single bout") {
            HomePage {
                singleBoutButton.click()
            }
        }

        step("Verify initial scores are 0-0") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
                rightScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }

        step("Score 5 touches for left fencer") {
            BoutPage {
                repeat(5) {
                    leftScoreButton.click()
                }
            }
        }

        step("Verify left fencer wins with score 5") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("5")
                leftWinner.withUseUnmergedTree(true).assertIsDisplayed()
            }
        }
    }

    @Test
    fun fullBoutCycle_rightWins() {
        step("Start from home and navigate to single bout") {
            HomePage {
                singleBoutButton.click()
            }
        }

        step("Score 5 touches for right fencer") {
            BoutPage {
                repeat(5) {
                    rightScoreButton.click()
                }
            }
        }

        step("Verify right fencer wins with score 5") {
            BoutPage {
                rightScore.withUseUnmergedTree(true).assertTextContains("5")
                rightWinner.withUseUnmergedTree(true).assertIsDisplayed()
            }
        }
    }

    @Test
    fun settingsAndBack_navigation() {
        step("Navigate from Home to Settings") {
            HomePage {
                settingsButton.click()
            }
        }

        step("Switch weapon and go back") {
            SettingsPage {
                foilEpeeChip.click()
                backButton.click()
            }
        }

        step("Verify returned to Home screen") {
            HomePage {
                singleBoutButton.assertIsDisplayed()
            }
        }
    }
}
