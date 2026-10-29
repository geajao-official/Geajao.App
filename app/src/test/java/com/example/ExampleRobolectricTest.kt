package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GeajaoConstants
import com.example.ui.components.FirstTimeUserGuidePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Geajao", appName)
    }

    @Test
    fun `first time user guide preferences and official contact infos`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(FirstTimeUserGuidePreferences.shouldShowFirstTimeGuide(context))

        FirstTimeUserGuidePreferences.markFirstTimeGuideCompleted(context)
        assertFalse(FirstTimeUserGuidePreferences.shouldShowFirstTimeGuide(context))

        assertEquals(
            "https://www.instagram.com/_.mukhosh._?mdxt=MnF6ank1c3Yyb3h5",
            GeajaoConstants.OFFICIAL_INSTAGRAM_URL
        )
        assertEquals(
            "geajao.official@gmail.com",
            GeajaoConstants.OFFICIAL_GMAIL_ADDRESS
        )
    }

    @Test
    fun `global dark mode theme toggle and Made By MUKHOSH logo preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.ui.theme.ThemePreferences.init(context)
        assertTrue(com.example.ui.theme.ThemePreferences.isDarkModeFlow.value)

        com.example.ui.theme.ThemePreferences.setDarkMode(context, false)
        assertFalse(com.example.ui.theme.ThemePreferences.isDarkModeFlow.value)

        val toggledBack = com.example.ui.theme.ThemePreferences.toggleDarkMode(context)
        assertTrue(toggledBack)
        assertTrue(com.example.ui.theme.ThemePreferences.isDarkModeFlow.value)

        assertEquals("Made By MUKHOSH", GeajaoConstants.MADE_BY_MUKHOSH_TAG)

        com.example.ui.components.MukhoshLogoPreferences.init(context)
        com.example.ui.components.MukhoshLogoPreferences.saveLocalMukhoshLogo(
            context,
            "data:image/jpeg;base64,MUKHOSH_LOGO_TEST"
        )
        assertEquals(
            "data:image/jpeg;base64,MUKHOSH_LOGO_TEST",
            com.example.ui.components.MukhoshLogoPreferences.localMukhoshLogoFlow.value
        )
    }

    @Test
    fun `voice message audio engine encodes and decodes playable voice note payload`() {
        val dataUri = com.example.ui.components.VoiceMessageAudioEngine.generateSynthesizedVoiceDataUri(
            durationMs = 4500L,
            waveformSeed = listOf(0.25f, 0.5f, 0.75f, 0.9f)
        )
        assertTrue(dataUri.startsWith("data:audio/wav;durationMs=4500;"))
        assertTrue(dataUri.length <= 150000)

        val parsed = com.example.ui.components.VoiceMessageAudioEngine.parseVoicePayload(
            rawMediaData = dataUri,
            fallbackLabel = "Voice Message (0:04)"
        )
        assertEquals("audio/wav", parsed.mimeType)
        assertEquals(4500L, parsed.durationMs)
        assertEquals(24, parsed.waveform.size)
        assertTrue(parsed.audioBytes.size > 44)
        assertEquals(
            "RIFF",
            String(parsed.audioBytes.copyOfRange(0, 4), Charsets.US_ASCII)
        )
        assertEquals(
            "0:04",
            com.example.ui.components.VoiceMessageAudioEngine.formatDurationMs(4500L)
        )
    }

    @Test
    fun `discovery feed trending tags college filters and multi badge resolution`() {
        assertTrue(GeajaoConstants.FEED_TABS.containsAll(listOf("Recent", "Trending", "Friends")))
        assertTrue(GeajaoConstants.FEED_TRENDING_TAGS.containsAll(listOf("General", "Trending", "AcademyLife", "Reunion")))
        assertEquals(12, GeajaoConstants.CADET_COLLEGES.size)
        assertEquals("CCR", GeajaoConstants.CADET_COLLEGES.first())
        assertEquals("JGCC", GeajaoConstants.CADET_COLLEGES.last())

        val multiBadgeProfile = com.example.data.model.CadetProfile(
            userId = "u1",
            displayName = "Cadet Commander",
            academyHouse = "CCR",
            verificationStatus = GeajaoConstants.STATUS_APPROVED,
            primaryBadge = GeajaoConstants.BADGE_ADMIN,
            badges = listOf(GeajaoConstants.BADGE_ADMIN, GeajaoConstants.BADGE_MODERATOR, GeajaoConstants.BADGE_EX_CADET)
        )
        assertEquals(
            listOf(GeajaoConstants.BADGE_ADMIN, GeajaoConstants.BADGE_MODERATOR, GeajaoConstants.BADGE_EX_CADET),
            multiBadgeProfile.allEffectiveBadges
        )
        assertEquals("Admin,Moderator,Ex-cadet", multiBadgeProfile.serializedBadgeSummary)
        assertEquals("CCR", multiBadgeProfile.cadetCollege)

        // Verify legacy House system strings are completely stripped and normalized to official Cadet Colleges
        assertEquals("CCR", GeajaoConstants.normalizeCadetCollege("Tariq House"))
        assertEquals("CCR", GeajaoConstants.normalizeCadetCollege("Khalid House"))
        assertEquals("FCC", GeajaoConstants.normalizeCadetCollege("FCC"))
        assertEquals("JGCC", GeajaoConstants.normalizeCadetCollege("jgcc"))
        assertEquals("CCR • Batch 54", GeajaoConstants.sanitizeBatchLabel("Tariq House • Batch 54"))
        assertEquals("FCC • Batch 52", GeajaoConstants.sanitizeBatchLabel("FCC • Khalid House • Batch 52"))
    }
}
