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

@Epic("Group Stage")
@Feature("Group Setup")
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
        step("Navigate to group setup screen") {
            navigateToGroupSetup()
        }
        step("Verify all fencer count options and create button are displayed") {
            GroupSetupPage {
                fencerCountChip(5).assertIsDisplayed()
                fencerCountChip(6).assertIsDisplayed()
                fencerCountChip(7).assertIsDisplayed()
                fencerCountChip(8).assertIsDisplayed()
                createButton.assertExists()
            }
        }
    }

    @Test
    fun groupSetup_changeFencerCount() {
        step("Navigate to group setup screen") {
            navigateToGroupSetup()
        }
        step("Select 6 fencers") {
            GroupSetupPage {
                fencerCountChip(6).click()
            }
        }
        step("Verify 6 name input fields exist") {
            GroupSetupPage {
                nameInput(5).assertExists()
            }
        }
    }

    @Test
    fun groupSetup_fillNamesAndCreate() {
        step("Navigate to group setup screen") {
            navigateToGroupSetup()
        }
        step("Fill names for 5 fencers") {
            GroupSetupPage {
                for (i in 0 until 5) {
                    nameInput(i).scrollTo()
                    nameInput(i).click()
                    nameInput(i).clearText()
                    nameInput(i).inputText("Fencer ${i + 1}")
                }
            }
        }
        step("Click create button") {
            GroupSetupPage {
                createButton.scrollTo()
                createButton.click()
            }
        }
        step("Verify navigation to dashboard") {
            GroupDashboardPage {
                progressText.withTimeout(15000).assertIsDisplayed()
            }
        }
    }
}
