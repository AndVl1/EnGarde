package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutConfirmPage
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import org.junit.Test

@Epic("Group Stage")
@Feature("Bout Confirm")
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
        step("Navigate to bout confirm screen") {
            navigateToBoutConfirm()
        }
        step("Verify all bout confirm elements are displayed") {
            BoutConfirmPage {
                boutNumber.assertIsDisplayed()
                leftName.assertIsDisplayed()
                rightName.assertIsDisplayed()
                startButton.assertIsDisplayed()
                cancelButton.assertIsDisplayed()
                swapButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun boutConfirm_swapSides() {
        step("Navigate to bout confirm screen") {
            navigateToBoutConfirm()
        }
        step("Get initial fencer names") {
            BoutConfirmPage {
                val leftNameBefore = leftName.getText() ?: ""
                val rightNameBefore = rightName.getText() ?: ""
                step("Click swap button") {
                    swapButton.click()
                }
                step("Verify fencer positions are swapped") {
                    leftName.assertTextContains(rightNameBefore)
                    rightName.assertTextContains(leftNameBefore)
                }
            }
        }
    }

    @Test
    fun boutConfirm_startBout() {
        step("Navigate to bout confirm screen") {
            navigateToBoutConfirm()
        }
        step("Click start button") {
            BoutConfirmPage {
                startButton.click()
            }
        }
        step("Verify bout screen is displayed") {
            BoutPage {
                timerBox.assertIsDisplayed()
                leftScoreButton.assertIsDisplayed()
                rightScoreButton.assertIsDisplayed()
            }
        }
    }

    @Test
    fun boutConfirm_cancel() {
        step("Navigate to bout confirm screen") {
            navigateToBoutConfirm()
        }
        step("Click cancel button") {
            BoutConfirmPage {
                cancelButton.click()
            }
        }
        step("Verify returned to dashboard") {
            GroupDashboardPage {
                startBoutButton.assertIsDisplayed()
            }
        }
    }
}
