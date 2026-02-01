package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutConfirmPage
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import org.junit.Test

class BoutConfirmScreenTest : BaseTest() {

    private fun navigateToBoutConfirm() {
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
        // Wait for Dashboard to load
        GroupDashboardPage {
            progressText.withTimeout(15000).assertIsDisplayed()
            startBoutButton.click()
        }
    }

    @Test
    fun boutConfirm_displaysParticipants() {
        navigateToBoutConfirm()
        BoutConfirmPage {
            boutNumber.assertIsDisplayed()
            leftName.assertIsDisplayed()
            rightName.assertIsDisplayed()
            startButton.assertIsDisplayed()
            cancelButton.assertIsDisplayed()
            swapButton.assertIsDisplayed()
        }
    }

    @Test
    fun boutConfirm_swapSides() {
        navigateToBoutConfirm()
        BoutConfirmPage {
            val leftNameBefore = leftName.getText() ?: ""
            val rightNameBefore = rightName.getText() ?: ""
            swapButton.click()
            leftName.assertTextContains(rightNameBefore)
            rightName.assertTextContains(leftNameBefore)
        }
    }

    @Test
    fun boutConfirm_startBout() {
        navigateToBoutConfirm()
        BoutConfirmPage {
            startButton.click()
        }
        BoutPage {
            timerBox.assertIsDisplayed()
            leftScoreButton.assertIsDisplayed()
            rightScoreButton.assertIsDisplayed()
        }
    }

    @Test
    fun boutConfirm_cancel() {
        navigateToBoutConfirm()
        BoutConfirmPage {
            cancelButton.click()
        }
        GroupDashboardPage {
            startBoutButton.assertIsDisplayed()
        }
    }
}
