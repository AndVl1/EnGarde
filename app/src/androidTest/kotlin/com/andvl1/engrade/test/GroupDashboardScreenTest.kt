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
}
