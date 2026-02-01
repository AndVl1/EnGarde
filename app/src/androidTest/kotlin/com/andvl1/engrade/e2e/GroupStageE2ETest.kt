package com.andvl1.engrade.e2e

import com.andvl1.engrade.base.BaseTest
import com.andvl1.engrade.page.BoutConfirmPage
import com.andvl1.engrade.page.BoutPage
import com.andvl1.engrade.page.GroupDashboardPage
import com.andvl1.engrade.page.GroupSetupPage
import com.andvl1.engrade.page.HomePage
import com.atiurin.ultron.extensions.*
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Allure.step
import org.junit.Test

@Epic("E2E")
@Feature("Group Stage Flow")
class GroupStageE2ETest : BaseTest() {

    @Test
    fun createGroupAndStartFirstBout() {
        step("Navigate from Home to Group Stage") {
            HomePage {
                groupStageButton.assertIsDisplayed()
                groupStageButton.click()
            }
        }

        step("Fill fencer names and create pool") {
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
        }

        step("Verify dashboard shows and start first bout") {
            GroupDashboardPage {
                progressText.withTimeout(15000).assertIsDisplayed()
                startBoutButton.assertIsDisplayed()
                startBoutButton.click()
            }
        }

        step("Confirm bout participants") {
            BoutConfirmPage {
                leftName.assertIsDisplayed()
                rightName.assertIsDisplayed()
                startButton.click()
            }
        }

        step("Verify bout screen displays with initial scores") {
            BoutPage {
                timerBox.assertIsDisplayed()
                leftScore.withUseUnmergedTree(true).assertTextContains("0")
                rightScore.withUseUnmergedTree(true).assertTextContains("0")
            }
        }
    }
}
