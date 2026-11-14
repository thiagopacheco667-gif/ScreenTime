package app.screentime.domain.color

import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetImageColorsTest {

    private fun c(hex: Long) = hex.toInt()

    /** Paleta próxima da foto de exemplo: cabelo escuro, sardas terracota, pele pêssego, tecido creme. */
    private val warm = ImagePalette(
        dark = c(0xFF2A2220), mid = c(0xFF9C6B57), vivid = c(0xFFC46A4E), light = c(0xFFE8DFCF), average = c(0xFFB8907A),
    )

    /** Foto quase branca: o pior caso para texto branco. */
    private val white = ImagePalette(
        dark = c(0xFFDDDDDD), mid = c(0xFFEEEEEE), vivid = c(0xFFF4F0E8), light = c(0xFFFFFFFF), average = c(0xFFF6F6F6),
    )

    /** Foto neon saturada. */
    private val neon = ImagePalette(
        dark = c(0xFF001A33), mid = c(0xFF00CCFF), vivid = c(0xFFFFFF00), light = c(0xFFCCFF66), average = c(0xFF66CC99),
    )

    private fun check(p: ImagePalette, dim: Int) {
        val colors = WidgetImageColors.from(p, dim)
        val brightest = ColorMath.towards(p.average, p.light, 0.5f)
        val bgs = listOf(
            ColorMath.blend(colors.overlayStart, brightest, colors.overlayAlpha),
            ColorMath.blend(colors.overlayEnd, brightest, colors.overlayAlpha),
        )
        bgs.forEach { bg ->
            assertTrue("texto", ColorMath.contrast(colors.text, bg) >= 4.5)
            assertTrue("secundário", ColorMath.contrast(colors.muted, bg) >= 4.5)
            assertTrue("destaque", ColorMath.contrast(colors.accent, bg) >= 4.5 - 0.3)
            assertTrue("barra", ColorMath.contrast(colors.barBottom, bg) >= 3.0 - 0.3)
        }
        assertTrue("selo", ColorMath.contrast(colors.text, colors.pillBackground) >= 4.5)
    }

    @Test fun `foto quente de exemplo e legivel`() = check(warm, 45)

    @Test fun `foto quase branca forca overlay mais escuro`() = check(white, 0)

    @Test fun `foto neon continua legivel`() = check(neon, 20)

    @Test
    fun `contraste basico`() {
        assertTrue(ColorMath.contrast(ColorMath.WHITE, ColorMath.BLACK) > 20.9)
        val fixed = ColorMath.ensureContrast(c(0xFFFFEE88), ColorMath.WHITE, 4.5)
        assertTrue(ColorMath.contrast(fixed, ColorMath.WHITE) >= 4.5)
    }
}
