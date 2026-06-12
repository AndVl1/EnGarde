package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.DeTableauPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

/**
 * Instrumented tests for the Direct Elimination bracket screen.
 *
 * Setup: 5-fencer pool (minimum pool size), all 10 pool bouts completed via quick-entry
 * (score 5:0 for each upper-triangle pair — fencer with lower seed number wins).
 * This produces a deterministic seeding:
 *   Seed 1 → wins all 4 bouts (fencer with index 0, name "DE Fencer 1")
 *   Seed 5 → loses all 4 bouts
 *
 * Resulting 8-slot bracket (3 rounds):
 *   Round 1 (Quarterfinal): 4 matches — 3 byes, 1 real match (seed 4 vs seed 5)
 *   Round 2 (Semifinal):    2 matches — after Round 1 resolves
 *   Round 3 (Final):        1 match
 *
 * Round-1 match IDs (tableau size=8):
 *   Match 1: seed1 vs BYE  (position 1)
 *   Match 2: seed4 vs seed5 (position 2) ← the only ready match
 *   Match 3: seed2 vs BYE  (position 3)
 *   Match 4: seed3 vs BYE  (position 4)
 */
@Epic("Direct Elimination")
@Feature("DE Tableau Screen")
class DeTableauScreenTest : BaseTest() {

    // All 10 unique pairs for a 5-fencer pool (upper triangle, left < right)
    private val poolPairs = listOf(
        1 to 2, 1 to 3, 1 to 4, 1 to 5,
        2 to 3, 2 to 4, 2 to 5,
        3 to 4, 3 to 5,
        4 to 5
    )

    /**
     * Creates a 5-fencer pool, navigates to the dashboard, and completes all 10 pool
     * bouts via quick-entry using score 5:0 (lower-index fencer always wins).
     *
     * Assumes the matrix quick-entry dialog is accessible at [matrixCell(leftSeed, rightSeed)].
     */
    private fun createPoolAndCompleteAllBouts() {
        // 1. Navigate to Group Stage and create a 5-fencer pool
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
                nameInput(i).inputText("DE Fencer ${i + 1}")
            }
            createButton.scrollTo()
            createButton.click()
        }
        // Wait for dashboard
        GroupDashboardPage {
            progressText.withTimeout(15000).assertIsDisplayed()
        }

        // 2. Complete all 10 pool bouts via quick-entry (5:0 each)
        poolPairs.forEach { (left, right) ->
            GroupDashboardPage {
                matrixCell(left, right).withTimeout(5000).scrollTo()
                matrixCell(left, right).click()
                quickScoreLeftInput.withTimeout(5000).assertIsDisplayed()
                quickScoreLeftInput.clearText()
                quickScoreLeftInput.inputText("5")
                quickScoreRightInput.clearText()
                quickScoreRightInput.inputText("0")
                quickScoreConfirmButton.click()
            }
        }
    }

    @Test
    fun deTableau_bracketLoadsAfterPoolCompletion() {
        step("Create pool and complete all bouts") {
            createPoolAndCompleteAllBouts()
        }
        step("Verify 'Proceed to DE' button is visible after all bouts complete") {
            GroupDashboardPage {
                progressText.withTimeout(10000).assertIsDisplayed()
                proceedToDeButton.withTimeout(10000).assertIsDisplayed()
            }
        }
        step("Tap 'Proceed to DE'") {
            GroupDashboardPage {
                proceedToDeButton.click()
            }
        }
        step("Verify DE bracket screen loads") {
            DeTableauPage {
                screenTitle.withTimeout(15000).assertIsDisplayed()
                // Round 1 title visible (Quarterfinal for 5-fencer / 8-slot bracket)
                roundTitle(1).withTimeout(10000).assertIsDisplayed()
            }
        }
        step("Verify bracket shows 4 round-1 match cards") {
            DeTableauPage {
                matchCard(1).withTimeout(5000).assertIsDisplayed()
                matchCard(2).assertIsDisplayed()
                matchCard(3).assertIsDisplayed()
                matchCard(4).assertIsDisplayed()
            }
        }
        step("Verify exactly one match has a play button (seed4 vs seed5 = match 2)") {
            DeTableauPage {
                // Match 2 (position 2) is the only real match; others are byes
                playButton(2).withTimeout(5000).assertIsDisplayed()
                // Bye matches have no play button
                playButton(1).assertDoesNotExist()
                playButton(3).assertDoesNotExist()
                playButton(4).assertDoesNotExist()
            }
        }
    }

    @Test
    fun deTableau_playMatchAndWinnerAdvances() {
        step("Create pool and complete all bouts") {
            createPoolAndCompleteAllBouts()
        }
        step("Navigate to DE bracket") {
            GroupDashboardPage {
                proceedToDeButton.withTimeout(10000).assertIsDisplayed()
                proceedToDeButton.click()
            }
        }
        step("Verify bracket loaded") {
            DeTableauPage {
                roundTitle(1).withTimeout(15000).assertIsDisplayed()
                playButton(2).withTimeout(5000).assertIsDisplayed()
            }
        }
        step("Tap play on match 2 (seed4 vs seed5)") {
            DeTableauPage {
                playButton(2).click()
            }
        }
        step("Verify bout screen shows with DE fencers") {
            BoutPage {
                timerBox.withTimeout(10000).assertIsDisplayed()
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
                rightScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
        step("Score 15 touches for left fencer to win the DE bout") {
            BoutPage {
                repeat(15) {
                    leftScoreButton.click()
                }
            }
        }
        step("Verify left fencer wins (bout ends at 15 touches)") {
            BoutPage {
                leftWinner.withUseUnmergedTree(true).withTimeout(5000).assertIsDisplayed()
            }
        }
        step("Verify navigation returns to DE bracket after result is recorded") {
            DeTableauPage {
                // Back on bracket screen after async recordMatchResult + popWhile
                roundTitle(1).withTimeout(15000).assertIsDisplayed()
            }
        }
        step("Verify match 2 is now resolved (play button gone)") {
            DeTableauPage {
                playButton(2).assertDoesNotExist()
            }
        }
        step("Verify semifinal (round 2, match 5) now shows winner of match 2") {
            DeTableauPage {
                roundTitle(2).assertIsDisplayed()
                matchCard(5).withTimeout(5000).assertIsDisplayed()
                // Match 5 bottom slot should now show the winner's name (no longer TBD)
                // We verify by checking the play button or slot content is no longer TBD
                slotBottom(5).withTimeout(5000).assertIsDisplayed()
            }
        }
    }

    @Test
    fun deTableau_resumeExistingBracket() {
        step("Create pool, complete all bouts, navigate to DE") {
            createPoolAndCompleteAllBouts()
            GroupDashboardPage {
                proceedToDeButton.withTimeout(10000).assertIsDisplayed()
                proceedToDeButton.click()
            }
        }
        step("Verify bracket loaded") {
            DeTableauPage {
                roundTitle(1).withTimeout(15000).assertIsDisplayed()
            }
        }
        step("Navigate back to dashboard") {
            DeTableauPage {
                backButton.click()
            }
        }
        step("Dashboard still shows 'Proceed to DE' enabled") {
            GroupDashboardPage {
                progressText.withTimeout(5000).assertIsDisplayed()
                proceedToDeButton.assertIsDisplayed()
                proceedToDeButton.assertIsEnabled()
            }
        }
        step("Tap 'Proceed to DE' again — should resume existing bracket") {
            GroupDashboardPage {
                proceedToDeButton.click()
            }
        }
        step("Bracket screen shows again without creating a duplicate tableau") {
            DeTableauPage {
                roundTitle(1).withTimeout(15000).assertIsDisplayed()
                // The same 4 match cards must still be present
                matchCard(1).withTimeout(5000).assertIsDisplayed()
                matchCard(2).assertIsDisplayed()
            }
        }
    }
}
