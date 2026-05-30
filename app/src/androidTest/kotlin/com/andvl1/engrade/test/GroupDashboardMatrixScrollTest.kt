package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

/**
 * Regression test for M5 — the FIE result matrix must scroll horizontally so that
 * every column is reachable for pools of 5-8 fencers (the matrix is wider than the
 * screen). Before the fix the matrix used .horizontalScroll().fillMaxWidth(), which
 * collapsed the content to screen width and disabled scrolling — the rightmost
 * columns were unreachable.
 *
 * The test creates a 6-fencer pool and asserts that the last column header
 * (matrix_header_col_6), which starts off-screen, becomes reachable via scrollTo().
 * If horizontal scroll is broken, scrollTo() / assertIsDisplayed() fails.
 */
@Epic("Group Stage")
@Feature("Group Dashboard")
class GroupDashboardMatrixScrollTest : BaseTest() {

    private fun createSixFencerPoolAndOpenDashboard() {
        HomePage {
            groupStageButton.assertIsDisplayed()
            groupStageButton.click()
        }
        GroupSetupPage {
            fencerCountChip(6).withTimeout(10000).assertIsDisplayed()
            fencerCountChip(6).click()
            for (i in 0 until 6) {
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
        }
    }

    @Test
    fun matrix_lastColumn_reachableViaHorizontalScroll() {
        step("Create 6-fencer pool and open dashboard") {
            createSixFencerPoolAndOpenDashboard()
        }
        step("Matrix is present") {
            GroupDashboardPage {
                matrixTitle.scrollTo().assertIsDisplayed()
                matrix.withTimeout(10000).assertExists()
            }
        }
        step("Last matrix column (6) is reachable via horizontal scroll") {
            GroupDashboardPage {
                // First column is on-screen initially; the 6th must require horizontal scroll.
                matrixHeaderCol(6).scrollTo().assertIsDisplayed()
            }
        }
        step("After scrolling to the end, the first column scrolled back is still reachable") {
            GroupDashboardPage {
                matrixHeaderCol(1).scrollTo().assertIsDisplayed()
            }
        }
    }
}
