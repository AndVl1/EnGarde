package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutConfirmPage
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.BoutResultPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

@Epic("Group Stage")
@Feature("Bout Result")
class BoutResultScreenTest : BaseTest() {

    /**
     * Navigates through the full group-stage flow up to the BoutScreen.
     * Caller is responsible for scoring inside BoutPage.
     */
    private fun navigateToBoutScreen() {
        HomePage {
            groupStageButton.assertIsDisplayed()
            groupStageButton.click()
        }
        GroupSetupPage {
            fencerCountChip(5).assertIsDisplayed()
            for (i in 0 until 5) {
                nameInput(i).scrollTo()
                nameInput(i).click()
                nameInput(i).clearText()
                nameInput(i).inputText("Fencer ${i + 1}")
            }
            createButton.scrollTo()
            createButton.click()
        }
        GroupDashboardPage {
            progressText.withTimeout(15000).assertIsDisplayed()
            startBoutButton.click()
        }
        BoutConfirmPage {
            startButton.assertIsDisplayed()
            startButton.click()
        }
        BoutPage {
            timerBox.assertIsDisplayed()
        }
    }

    @Test
    fun boutResult_leftWins_displaysCorrectScoreAndWinner() {
        step("Navigate to bout screen inside a pool") {
            navigateToBoutScreen()
        }
        step("Score 3 for right fencer, then 5 for left fencer") {
            BoutPage {
                // Right scores 3 first so bout doesn't end early
                repeat(3) { rightScoreButton.click() }
                // Left scores 5 — bout ends at 5-3, left wins
                repeat(5) { leftScoreButton.click() }
            }
        }
        step("Verify bout result screen shows left as winner with score V5 / D3") {
            BoutResultPage {
                title.withTimeout(5000).assertIsDisplayed()
                // Material3 Card merges semantics — use unmerged tree to reach inner Text nodes
                leftScore.withUseUnmergedTree(true).assertTextContains("V5")
                rightScore.withUseUnmergedTree(true).assertTextContains("D3")
                continueButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun boutResult_rightWins_displaysCorrectScoreAndWinner() {
        step("Navigate to bout screen inside a pool") {
            navigateToBoutScreen()
        }
        step("Score 5 for right fencer — right wins 5-0") {
            BoutPage {
                repeat(5) { rightScoreButton.click() }
            }
        }
        step("Verify bout result screen shows right as winner with score D0 / V5") {
            BoutResultPage {
                title.withTimeout(5000).assertIsDisplayed()
                leftScore.withUseUnmergedTree(true).assertTextContains("D0")
                rightScore.withUseUnmergedTree(true).assertTextContains("V5")
                continueButton.assertIsDisplayed()
            }
        }
    }
}
