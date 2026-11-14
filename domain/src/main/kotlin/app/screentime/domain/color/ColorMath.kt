package app.screentime.domain.color

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Contas de cor em ARGB (Int), sem Android, para poderem ser testadas.
 * Contraste conforme a WCAG: 4,5:1 para texto, 3:1 para elementos gráficos.
 */
object ColorMath {
    const val WHITE: Int = 0xFFFFFFFF.toInt()
    const val BLACK: Int = 0xFF000000.toInt()

    fun alpha(c: Int) = (c ushr 24) and 0xFF
    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF

    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun withAlpha(c: Int, a: Float): Int = argb((a.coerceIn(0f, 1f) * 255).roundToInt(), red(c), green(c), blue(c))

    /** Luminância relativa (0 = preto, 1 = branco). */
    fun luminance(c: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(red(c)) + 0.7152 * ch(green(c)) + 0.0722 * ch(blue(c))
    }

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** [fg] por cima de [bg] com opacidade [alpha]; resultado opaco. */
    fun blend(fg: Int, bg: Int, alpha: Float): Int {
        val t = alpha.coerceIn(0f, 1f)
        fun mix(f: Int, b: Int) = (f * t + b * (1 - t)).roundToInt()
        return argb(255, mix(red(fg), red(bg)), mix(green(fg), green(bg)), mix(blue(fg), blue(bg)))
    }

    /** Aproxima [c] de [target] na fração [t] (0..1). */
    fun towards(c: Int, target: Int, t: Float): Int = blend(target, c, t)

    /**
     * Ajusta [fg] até ter contraste [min] com [bg], indo para o branco ou para o preto
     * (o lado que tiver mais espaço). Mantém o tom da cor o quanto for possível.
     */
    fun ensureContrast(fg: Int, bg: Int, min: Double): Int {
        if (contrast(fg, bg) >= min) return opaque(fg)
        val target = if (contrast(WHITE, bg) >= contrast(BLACK, bg)) WHITE else BLACK
        var t = 0f
        var c = opaque(fg)
        while (t < 1f && contrast(c, bg) < min) {
            t += 0.05f
            c = towards(opaque(fg), target, t)
        }
        return c
    }

    private fun opaque(c: Int) = c or (0xFF shl 24)
}
