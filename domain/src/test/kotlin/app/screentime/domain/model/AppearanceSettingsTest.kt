package app.screentime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceSettingsTest {

    @Test
    fun `padrao segue a maquete aprovada`() {
        val s = AppearanceSettings()
        assertEquals(ThemeMode.Dark, s.themeMode)
        assertEquals(AccentColor.Gold, s.accent)
        assertEquals(FontChoice.App, s.font)
        assertEquals(4, s.widgetAppCount)
    }

    @Test
    fun `valores fora da faixa sao corrigidos`() {
        val s = AppearanceSettings(
            widgetAppCount = 9,
            widgetImage = WidgetImageSettings(dimPercent = 150, blurPx = -3),
        ).normalized()
        assertEquals(4, s.widgetAppCount)
        assertEquals(80, s.widgetImage.dimPercent)
        assertEquals(0, s.widgetImage.blurPx)
    }

    @Test
    fun `imagem so vale nos widgets escolhidos e com imagem salva`() {
        val base = AppearanceSettings(
            widgetStyle = WidgetStyle.Image,
            widgetImage = WidgetImageSettings(hasImage = true, target = WidgetImageTarget.Square),
        )
        assertEquals(WidgetStyle.Image, base.styleFor(WidgetKind.Square))
        assertEquals(WidgetStyle.Navy, base.styleFor(WidgetKind.Expanded))

        val noImage = base.copy(widgetImage = base.widgetImage.copy(hasImage = false))
        assertEquals(WidgetStyle.Navy, noImage.styleFor(WidgetKind.Square))

        val all = base.copy(widgetImage = base.widgetImage.copy(target = WidgetImageTarget.All))
        assertEquals(WidgetStyle.Image, all.styleFor(WidgetKind.Compact))

        assertEquals(WidgetStyle.Light, AppearanceSettings(widgetStyle = WidgetStyle.Light).styleFor(WidgetKind.Square))
    }
}
