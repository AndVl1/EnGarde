package com.andvl1.engrade.test

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import org.junit.Test

class GroupSetupScreenTest : BaseTest() {

    private fun navigateToGroupSetup() {
        HomePage {
            groupStageButton.assertIsDisplayed()
            groupStageButton.assertIsEnabled()
            groupStageButton.click()
        }
        // Wait for GroupSetup screen to load
        GroupSetupPage {
            // Try checking for a chip first (simpler element at top of screen)
            fencerCountChip(5).assertIsDisplayed()
        }
    }

    @Test
    fun groupSetup_displaysInitialState() {
        navigateToGroupSetup()
        GroupSetupPage {
            fencerCountChip(5).assertIsDisplayed()
            fencerCountChip(6).assertIsDisplayed()
            fencerCountChip(7).assertIsDisplayed()
            fencerCountChip(8).assertIsDisplayed()
            createButton.assertExists()
        }
    }

    @Test
    fun groupSetup_changeFencerCount() {
        navigateToGroupSetup()
        GroupSetupPage {
            fencerCountChip(6).click()
            // After selecting 6, 6 name inputs should appear (may be off-screen)
            nameInput(5).assertExists()
        }
    }

    @Test
    fun groupSetup_fillNamesAndCreate() {
        navigateToGroupSetup()
        GroupSetupPage {
            // Default is 5 fencers
            for (i in 0 until 5) {
                nameInput(i).scrollTo()
                nameInput(i).click()
                nameInput(i).clearText()
                nameInput(i).inputText("Fencer ${i + 1}")
            }
            createButton.scrollTo()
            createButton.click()
        }
        // Should navigate to Dashboard
        GroupDashboardPage {
            progressText.withTimeout(15000).assertIsDisplayed()
        }
    }
}
