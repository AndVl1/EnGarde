package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutConfirmPage
import com.andvl1.engrade.page.BoutsListPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import org.junit.Test

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
        createPoolAndNavigateToDashboard()
        GroupDashboardPage {
            progressText.withTimeout(15000).assertIsDisplayed()
            matrixTitle.assertIsDisplayed()
            rankingsTitle.assertIsDisplayed()
            startBoutButton.assertIsDisplayed()
        }
    }

    @Test
    fun dashboard_startNextBout() {
        createPoolAndNavigateToDashboard()
        GroupDashboardPage {
            startBoutButton.assertIsDisplayed()
            startBoutButton.click()
        }
        BoutConfirmPage {
            leftName.assertIsDisplayed()
            rightName.assertIsDisplayed()
            startButton.assertIsDisplayed()
        }
    }

    @Test
    fun dashboard_navigateToBoutsList() {
        createPoolAndNavigateToDashboard()
        GroupDashboardPage {
            boutsListButton.assertIsDisplayed()
            boutsListButton.click()
        }
        BoutsListPage {
            list.assertIsDisplayed()
        }
    }
}
