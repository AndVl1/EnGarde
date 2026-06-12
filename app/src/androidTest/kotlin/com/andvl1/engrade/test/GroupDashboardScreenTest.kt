package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutConfirmPage
import com.andvl1.engrade.page.BoutsListPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

@Epic("Group Stage")
@Feature("Group Dashboard")
class GroupDashboardScreenTest : BaseTest() {

    private fun createPoolAndNavigateToDashboard() {
        HomePage {
            groupStageButton.assertIsDisplayed()
            groupStageButton.click()
        }
        // Wait for GroupSetup screen to load
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
        // Wait for dashboard to load after pool creation
        GroupDashboardPage {
            progressText.withTimeout(15000).assertIsDisplayed()
        }
    }

    @Test
    fun dashboard_displaysAfterPoolCreation() {
        step("Create pool and navigate to dashboard") {
            createPoolAndNavigateToDashboard()
        }
        step("Verify all dashboard elements are displayed") {
            GroupDashboardPage {
                progressText.withTimeout(15000).assertIsDisplayed()
                matrixTitle.assertIsDisplayed()
                rankingsTitle.assertIsDisplayed()
                startBoutButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun dashboard_startNextBout() {
        step("Create pool and navigate to dashboard") {
            createPoolAndNavigateToDashboard()
        }
        step("Click start bout button") {
            GroupDashboardPage {
                startBoutButton.assertIsDisplayed()
                startBoutButton.click()
            }
        }
        step("Verify bout confirm screen is displayed") {
            BoutConfirmPage {
                leftName.assertIsDisplayed()
                rightName.assertIsDisplayed()
                startButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun dashboard_navigateToBoutsList() {
        step("Create pool and navigate to dashboard") {
            createPoolAndNavigateToDashboard()
        }
        step("Click bouts list button") {
            GroupDashboardPage {
                boutsListButton.assertIsDisplayed()
                boutsListButton.click()
            }
        }
        step("Verify bouts list screen is displayed") {
            BoutsListPage {
                list.assertIsDisplayed()
            }
        }
    }

    @Test
    fun quickEntry_validScoreRecords() {
        step("Create pool and navigate to dashboard") {
            createPoolAndNavigateToDashboard()
        }
        step("Wait for matrix to load and tap a pending cell") {
            GroupDashboardPage {
                // Matrix is loaded once progressText is visible
                progressText.withTimeout(15000).assertIsDisplayed()
                // Tap pending cell for fencer 1 vs fencer 2 (row=1, col=2)
                matrixCell(1, 2).withTimeout(5000).assertIsDisplayed()
                matrixCell(1, 2).click()
            }
        }
        step("Quick entry dialog is shown") {
            GroupDashboardPage {
                quickScoreLeftInput.withTimeout(5000).assertIsDisplayed()
                quickScoreRightInput.assertIsDisplayed()
                quickScoreConfirmButton.assertIsDisplayed()
            }
        }
        step("Enter valid non-draw scores and confirm") {
            GroupDashboardPage {
                quickScoreLeftInput.clearText()
                quickScoreLeftInput.inputText("5")
                quickScoreRightInput.clearText()
                quickScoreRightInput.inputText("3")
                quickScoreConfirmButton.click()
            }
        }
        step("Cell [1][2] is now completed with a score") {
            GroupDashboardPage {
                // Score text appears once Room flow re-emits and state recomposes
                matrixScore(1, 2).withTimeout(10000).assertIsDisplayed()
            }
        }
        step("Primary timer-flow start button is still present") {
            GroupDashboardPage {
                startBoutButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun excludeFencer_secondStepConfirmationRequired() {
        step("Create pool and navigate to dashboard") {
            createPoolAndNavigateToDashboard()
        }
        step("Open overflow menu") {
            GroupDashboardPage {
                overflowButton.withTimeout(5000).assertIsDisplayed()
                overflowButton.click()
            }
        }
        step("Tap Exclude menu item") {
            GroupDashboardPage {
                excludeMenuItem.withTimeout(3000).assertIsDisplayed()
                excludeMenuItem.click()
            }
        }
        step("Tap the first fencer in the first-step exclude dialog") {
            GroupDashboardPage {
                excludeFencerButton(1).withTimeout(3000).assertIsDisplayed()
                excludeFencerButton(1).click()
            }
        }
        step("Verify second-step confirmation dialog with fencer name appears") {
            GroupDashboardPage {
                // Presence of the tagged confirm-dialog text proves the second step triggered
                excludeConfirmText.withTimeout(3000).assertIsDisplayed()
            }
        }
    }

    @Test
    fun quickEntry_drawIsRejected() {
        step("Create pool and navigate to dashboard") {
            createPoolAndNavigateToDashboard()
        }
        step("Wait for matrix and tap a pending cell") {
            GroupDashboardPage {
                progressText.withTimeout(15000).assertIsDisplayed()
                matrixCell(1, 2).withTimeout(5000).assertIsDisplayed()
                matrixCell(1, 2).click()
            }
        }
        step("Quick entry dialog is shown") {
            GroupDashboardPage {
                quickScoreLeftInput.withTimeout(5000).assertIsDisplayed()
            }
        }
        step("Enter equal (draw) scores and attempt confirm") {
            GroupDashboardPage {
                quickScoreLeftInput.clearText()
                quickScoreLeftInput.inputText("5")
                quickScoreRightInput.clearText()
                quickScoreRightInput.inputText("5")
                quickScoreConfirmButton.click()
            }
        }
        step("Dialog remains open - draw was rejected") {
            GroupDashboardPage {
                // Confirm button still visible means dialog was not dismissed
                quickScoreConfirmButton.withTimeout(3000).assertIsDisplayed()
            }
        }
        step("Dismiss dialog and verify cell is still pending (no score text)") {
            GroupDashboardPage {
                quickScoreCancelButton.click()
                // Score text node must not exist for the cell
                matrixScore(1, 2).assertDoesNotExist()
            }
        }
    }
}
