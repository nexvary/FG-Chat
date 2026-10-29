package com.fgchat.app

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

/** Run on a disposable emulator with clean app data. */
class AppSmokeTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun identityNavigationPersistenceAndArabic() {
        rule.waitUntil(15000) { rule.onAllNodesWithText("Create identity").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Your name").performTextInput("FG Test")
        rule.onNodeWithText("Create identity").performClick()
        rule.waitUntil(20000) { rule.onAllNodesWithText("Chats").fetchSemanticsNodes().isNotEmpty() }
        screenshot("chats-en.png")
        rule.onNodeWithTag("nav_identity").performClick()
        rule.onNodeWithText("FG Test").performScrollTo().assertIsDisplayed()
        screenshot("identity-en.png")
        rule.onNodeWithText("Copy invitation").performScrollTo().assertIsDisplayed()
        val activityBeforeInvite=rule.activity
        val invitation=com.fgchat.tox.ToxEngine().use { "fgchat://add/${it.address()}" }
        fun openInvitation() {
            rule.runOnUiThread {
                rule.activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(invitation)).apply {
                    setPackage(rule.activity.packageName)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            rule.waitUntil(15000) {rule.onAllNodesWithText(invitation).fetchSemanticsNodes().isNotEmpty()}
            org.junit.Assert.assertSame("Warm invitation must reuse the existing identity owner",activityBeforeInvite,rule.activity)
        }
        openInvitation()
        rule.onNodeWithTag("nav_chats").performClick()
        openInvitation() // An identical link must route again after leaving the add screen.
        rule.activityRule.scenario.recreate()
        rule.waitUntil(15000) { rule.onAllNodesWithText("My identity").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("nav_identity").performClick()
        rule.onNodeWithText("FG Test").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("nav_settings").performClick()
        rule.onNodeWithText("Privacy and security").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onAllNodesWithText("Chats").onFirst().assertExists()
        rule.runOnUiThread {
            rule.activity.getSystemService(LocaleManager::class.java).applicationLocales=LocaleList.forLanguageTags("ar-EG")
        }
        rule.waitUntil(15000) { rule.onAllNodesWithText("المحادثات").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("المحادثات").assertCountEquals(2)
        screenshot("chats-ar.png")
        rule.onNodeWithTag("nav_identity").performClick()
        rule.onNodeWithText("FG Test").performScrollTo().assertIsDisplayed()
        screenshot("identity-ar.png")
    }
    private fun screenshot(name: String) {
        rule.waitForIdle()
        val directory="/sdcard/Download/fgchat-test-evidence"
        shell("mkdir -p $directory")
        shell("screencap -p $directory/$name")
        check(shell("ls -l $directory/$name").contains(name)) { "Screenshot was not saved: $name" }
    }
    private fun shell(command: String): String {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }
}
