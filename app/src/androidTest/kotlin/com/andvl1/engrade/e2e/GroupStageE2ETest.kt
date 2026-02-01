package com.andvl1.engrade.e2e

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutConfirmPage
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import org.junit.Test

class GroupStageE2ETest : BaseTest() {

    @Test
    fun createGroupAndStartFirstBout() {
        // 1. Home → Group Stage
        HomePage {
            groupStageButton.assertIsDisplayed()
            groupStageButton.click()
        }

        // 2. Fill names and create pool
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

        // 3. Dashboard should show
        GroupDashboardPage {
            progressText.withTimeout(15000).assertIsDisplayed()
            startBoutButton.assertIsDisplayed()
            startBoutButton.click()
        }

        // 4. Bout confirm
        BoutConfirmPage {
            leftName.assertIsDisplayed()
            rightName.assertIsDisplayed()
            startButton.click()
        }

        // 5. Bout screen with correct names
        BoutPage {
            timerBox.assertIsDisplayed()
            leftScore.withUseUnmergedTree(true).assertTextContains("0")
            rightScore.withUseUnmergedTree(true).assertTextContains("0")
        }
    }
}
