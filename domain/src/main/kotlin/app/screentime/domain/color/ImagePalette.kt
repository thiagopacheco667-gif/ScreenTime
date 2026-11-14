package app.screentime.domain.color

/**
 * Cores principais de uma imagem (ARGB), extraídas no aparelho.
 * @param dark tom escuro (sombras, cabelo); @param mid tom intermediário;
 * @param vivid tom mais vivo; @param light tom claro; @param average cor média da imagem.
 */
data class ImagePalette(
    val dark: Int,
    val mid: Int,
    val vivid: Int,
    val light: Int,
    val average: Int,
)

/** Cores finais do widget com fundo de imagem, todas já com contraste verificado. */
data class WidgetImageColors(
    val overlayStart: Int,
    val overlayEnd: Int,
    val overlayAlpha: Float,
    val text: Int,
    val muted: Int,
    val accent: Int,
    val barTop: Int,
    val barBottom: Int,
    val barNow: Int,
    val pillBackground: Int,
) {
    companion object {
        private const val TEXT = 4.5
        private const val GRAPHIC = 3.0
        private const val MIN_ALPHA = 0.30f
        private const val MAX_ALPHA = 0.92f

        /**
         * Monta as cores a partir da [palette]. O degradê vai do tom escuro da imagem
         * a um tom intermediário (escuro + vivo). O escurecimento ([dimPercent]) é o
         * ponto de partida; se o texto branco não tiver contraste 4,5:1 no pior caso
         * (o fundo mais claro possível), a opacidade sobe até ter.
         */
        fun from(palette: ImagePalette, dimPercent: Int): WidgetImageColors {
            val start = ColorMath.towards(palette.dark, ColorMath.BLACK, 0.35f)
            var end = ColorMath.towards(ColorMath.blend(palette.vivid, palette.dark, 0.45f), ColorMath.BLACK, 0.25f)
            // O fim do degradê precisa ser escuro o bastante para o texto branco.
            end = ColorMath.ensureContrast(end, ColorMath.WHITE, 7.0).let { if (ColorMath.contrast(it, ColorMath.WHITE) >= 7.0) it else ColorMath.BLACK }

            // Pior caso do fundo: a parte mais clara da foto (média clareada) sob o overlay.
            val brightest = ColorMath.towards(palette.average, palette.light, 0.5f)
            var alpha = (dimPercent / 100f).coerceIn(MIN_ALPHA, MAX_ALPHA)
            fun worst(a: Float): Int {
                val s = ColorMath.blend(start, brightest, a)
                val e = ColorMath.blend(end, brightest, a)
                return if (ColorMath.luminance(s) > ColorMath.luminance(e)) s else e
            }
            while (alpha < MAX_ALPHA && ColorMath.contrast(ColorMath.WHITE, worst(alpha)) < TEXT) alpha += 0.02f
            alpha = alpha.coerceAtMost(MAX_ALPHA)
            val bg = worst(alpha)

            val muted = ColorMath.ensureContrast(ColorMath.blend(ColorMath.WHITE, bg, 0.78f), bg, TEXT)
            val accent = ColorMath.ensureContrast(ColorMath.towards(palette.vivid, ColorMath.WHITE, 0.15f), bg, TEXT)
            val barBottom = ColorMath.ensureContrast(palette.vivid, bg, GRAPHIC)
            val barTop = ColorMath.ensureContrast(ColorMath.towards(palette.light, ColorMath.WHITE, 0.1f), bg, GRAPHIC)
            var pill = ColorMath.blend(palette.vivid, bg, 0.55f)
            var t = 0f
            while (ColorMath.contrast(ColorMath.WHITE, pill) < TEXT && t < 1f) {
                t += 0.05f
                pill = ColorMath.towards(ColorMath.blend(palette.vivid, bg, 0.55f), ColorMath.BLACK, t)
            }

            return WidgetImageColors(
                overlayStart = start,
                overlayEnd = end,
                overlayAlpha = alpha,
                text = ColorMath.WHITE,
                muted = muted,
                accent = accent,
                barTop = barTop,
                barBottom = barBottom,
                barNow = ColorMath.WHITE,
                pillBackground = pill,
            )
        }
    }
}
