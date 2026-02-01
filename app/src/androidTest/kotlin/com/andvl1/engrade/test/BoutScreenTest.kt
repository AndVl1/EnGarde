package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import org.junit.Test

class BoutScreenTest : BaseTest() {

    private fun navigateToBout() {
        HomePage {
            singleBoutButton.click()
        }
    }

    @Test
    fun boutScreen_initialState() {
        navigateToBout()
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

    @Test
    fun boutScreen_leftScoreIncrement() {
        navigateToBout()
        BoutPage {
            leftScoreButton.click()
            leftScore.withUseUnmergedTree(true).assertTextContains("1")
            rightScore.withUseUnmergedTree(true).assertTextContains("0")
        }
    }

    @Test
    fun boutScreen_rightScoreIncrement() {
        navigateToBout()
        BoutPage {
            rightScoreButton.click()
            rightScore.withUseUnmergedTree(true).assertTextContains("1")
            leftScore.withUseUnmergedTree(true).assertTextContains("0")
        }
    }

    @Test
    fun boutScreen_undoAction() {
        navigateToBout()
        BoutPage {
            leftScoreButton.click()
            leftScore.withUseUnmergedTree(true).assertTextContains("1")
            undoButton.click()
            leftScore.withUseUnmergedTree(true).assertTextContains("0")
        }
    }

    @Test
    fun boutScreen_resetBout() {
        navigateToBout()
        BoutPage {
            leftScoreButton.click()
            rightScoreButton.click()
            leftScore.withUseUnmergedTree(true).assertTextContains("1")
            rightScore.withUseUnmergedTree(true).assertTextContains("1")
            resetButton.click()
            leftScore.withUseUnmergedTree(true).assertTextContains("0")
            rightScore.withUseUnmergedTree(true).assertTextContains("0")
        }
    }

    @Test
    fun boutScreen_boutEndsByMaxScore() {
        navigateToBout()
        BoutPage {
            // Score 5 touches for left fencer (default mode = 5)
            repeat(5) {
                leftScoreButton.click()
            }
            leftScore.withUseUnmergedTree(true).assertTextContains("5")
            leftWinner.withUseUnmergedTree(true).assertIsDisplayed()
        }
    }

    @Test
    fun boutScreen_skipSection() {
        navigateToBout()
        BoutPage {
            skipSectionButton.click()
            // After skip, section should change
            sectionTitle.assertIsDisplayed()
        }
    }

    @Test
    fun boutScreen_yellowCard() {
        navigateToBout()
        BoutPage {
            leftCardButton.click()
            yellowCardButton.assertIsDisplayed()
            yellowCardButton.click()
            // Yellow card doesn't add penalty score
            leftScore.withUseUnmergedTree(true).assertTextContains("0")
        }
    }

    @Test
    fun boutScreen_redCard() {
        navigateToBout()
        BoutPage {
            leftCardButton.click()
            redCardButton.assertIsDisplayed()
            redCardButton.click()
            // Red card gives penalty point to opponent
            rightScore.withUseUnmergedTree(true).assertTextContains("1")
        }
    }

    @Test
    fun boutScreen_cancelCardDialog() {
        navigateToBout()
        BoutPage {
            leftCardButton.click()
            yellowCardButton.assertIsDisplayed()
            cancelCardButton.click()
            // Dialog should be dismissed, scores unchanged
            leftScore.withUseUnmergedTree(true).assertTextContains("0")
            rightScore.withUseUnmergedTree(true).assertTextContains("0")
        }
    }
}
