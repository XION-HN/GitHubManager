package com.github.manager.ui.i18n

import org.junit.Assert.*
import org.junit.Test

class I18nStringsTest {

    @Test
    fun `BilingualText stores both languages`() {
        val text = BilingualText(zh = "你好", en = "Hello")
        assertEquals("你好", text.zh)
        assertEquals("Hello", text.en)
    }

    @Test
    fun `getText returns Chinese in CHINESE mode`() {
        languageModeState.value = LanguageMode.CHINESE
        val text = BilingualText(zh = "登录", en = "Sign In")
        assertEquals("登录", getText(text))
    }

    @Test
    fun `getText returns English in ENGLISH mode`() {
        languageModeState.value = LanguageMode.ENGLISH
        val text = BilingualText(zh = "登录", en = "Sign In")
        assertEquals("Sign In", getText(text))
    }

    @Test
    fun `getText returns Chinese in BILINGUAL mode`() {
        languageModeState.value = LanguageMode.BILINGUAL
        val text = BilingualText(zh = "登录", en = "Sign In")
        assertEquals("登录", getText(text))
    }

    @Test
    fun `I18nStrings appName has correct values`() {
        assertEquals("GitHub 管理器", I18nStrings.appName.zh)
        assertEquals("GitHub Manager", I18nStrings.appName.en)
    }

    @Test
    fun `I18nStrings signIn has correct values`() {
        assertEquals("登录", I18nStrings.signIn.zh)
        assertEquals("Sign In", I18nStrings.signIn.en)
    }

    @Test
    fun `I18nStrings all entries are non-blank`() {
        val fields = I18nStrings::class.java.declaredFields.filter { it.type == BilingualText::class.java }
        fields.forEach { field ->
            field.isAccessible = true
            val text = field.get(I18nStrings) as BilingualText
            assertTrue("Field ${field.name}.zh should not be blank", text.zh.isNotBlank())
            assertTrue("Field ${field.name}.en should not be blank", text.en.isNotBlank())
        }
    }

    @Test
    fun `LanguageMode enum has three values`() {
        assertEquals(3, LanguageMode.values().size)
        assertArrayEquals(arrayOf(LanguageMode.CHINESE, LanguageMode.ENGLISH, LanguageMode.BILINGUAL), LanguageMode.values())
    }

    @Test
    fun `ThemeMode enum has three values`() {
        assertEquals(3, ThemeMode.values().size)
        assertArrayEquals(arrayOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK), ThemeMode.values())
    }

    @Test
    fun `languageModeState default is BILINGUAL`() {
        assertEquals(LanguageMode.BILINGUAL, languageModeState.value)
    }

    @Test
    fun `themeModeState default is SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, themeModeState.value)
    }

    @Test
    fun `getText switches correctly when language changes`() {
        val text = BilingualText(zh = "仓库", en = "Repos")

        languageModeState.value = LanguageMode.CHINESE
        assertEquals("仓库", getText(text))

        languageModeState.value = LanguageMode.ENGLISH
        assertEquals("Repos", getText(text))

        languageModeState.value = LanguageMode.BILINGUAL
        assertEquals("仓库", getText(text))
    }

    @Test
    fun `I18nStrings key bilingual strings exist`() {
        assertNotNull(I18nStrings.tokenHelp)
        assertNotNull(I18nStrings.deleteRepoConfirm)
        assertNotNull(I18nStrings.logoutConfirm)
        assertTrue(I18nStrings.tokenHelp.zh.contains("设置"))
        assertTrue(I18nStrings.tokenHelp.en.contains("Settings"))
    }
}
