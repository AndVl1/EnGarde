package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

@Epic("Bout")
@Feature("Bout Screen")
class BoutScreenTest : BaseTest() {

    private fun navigateToBout() {
        HomePage {
            singleBoutButton.click()
        }
    }

    @Test
    fun boutScreen_initialState() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Verify initial bout state") {
            BoutPage {
                timerBox.assertIsDisplayed()
                leftScoreButton.assertIsDisplayed()
                rightScoreButton.assertIsDisplayed()
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
                rightScore.withUseUnmergedTree(true).assertTextContains("0")
                leftName.withUseUnmergedTree(true).assertExists()
                rightName.withUseUnmergedTree(true).assertExists()
            }
        }
    }

    @Test
    fun boutScreen_leftScoreIncrement() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Increment left fencer score") {
            BoutPage {
                leftScoreButton.click()
            }
        }
        step("Verify left score increased to 1, right score remains 0") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("1")
                rightScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
    }

    @Test
    fun boutScreen_rightScoreIncrement() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Increment right fencer score") {
            BoutPage {
                rightScoreButton.click()
            }
        }
        step("Verify right score increased to 1, left score remains 0") {
            BoutPage {
                rightScore.withUseUnmergedTree(true).assertTextContains("1")
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
    }

    @Test
    fun boutScreen_undoAction() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Increment left fencer score") {
            BoutPage {
                leftScoreButton.click()
                leftScore.withUseUnmergedTree(true).assertTextContains("1")
            }
        }
        step("Undo last action") {
            BoutPage {
                undoButton.click()
            }
        }
        step("Verify score reset to 0") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
    }

    @Test
    fun boutScreen_resetBout() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Add scores for both fencers") {
            BoutPage {
                leftScoreButton.click()
                rightScoreButton.click()
                leftScore.withUseUnmergedTree(true).assertTextContains("1")
                rightScore.withUseUnmergedTree(true).assertTextContains("1")
            }
        }
        step("Reset bout") {
            BoutPage {
                resetButton.click()
            }
        }
        step("Verify both scores reset to 0") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
                rightScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
    }

    @Test
    fun boutScreen_boutEndsByMaxScore() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Score 5 touches for left fencer") {
            BoutPage {
                repeat(5) {
                    leftScoreButton.click()
                }
            }
        }
        step("Verify bout ended with left fencer as winner") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("5")
                leftWinner.withUseUnmergedTree(true).assertIsDisplayed()
            }
        }
    }

    @Test
    fun boutScreen_skipSection() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Skip current section") {
            BoutPage {
                skipSectionButton.click()
            }
        }
        step("Verify section changed") {
            BoutPage {
                sectionTitle.assertIsDisplayed()
            }
        }
    }

    @Test
    fun boutScreen_yellowCard() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Open card dialog and select yellow card") {
            BoutPage {
                leftCardButton.click()
                yellowCardButton.assertIsDisplayed()
                yellowCardButton.click()
            }
        }
        step("Verify yellow card does not add penalty score") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
    }

    @Test
    fun boutScreen_redCard() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Open card dialog and select red card") {
            BoutPage {
                leftCardButton.click()
                redCardButton.assertIsDisplayed()
                redCardButton.click()
            }
        }
        step("Verify red card gives penalty point to opponent") {
            BoutPage {
                rightScore.withUseUnmergedTree(true).assertTextContains("1")
            }
        }
    }

    @Test
    fun boutScreen_cancelCardDialog() {
        step("Navigate to bout screen") {
            navigateToBout()
        }
        step("Open card dialog and cancel") {
            BoutPage {
                leftCardButton.click()
                yellowCardButton.assertIsDisplayed()
                cancelCardButton.click()
            }
        }
        step("Verify scores remain unchanged") {
            BoutPage {
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
                rightScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
    }
}
