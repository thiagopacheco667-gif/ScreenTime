package app.screentime.domain.model

enum class ThemeMode { System, Light, Dark }

enum class AccentColor { Gold, Blue, Green, Coral, Lilac }

enum class FontChoice {
    /** Manrope, incluída no app: igual em qualquer celular. */
    App,

    /** Fonte escolhida no sistema (por exemplo, no tema do HyperOS). */
    System,
}

enum class WidgetStyle { Navy, Light, Transparent, Image }

/** Em quais widgets a imagem de fundo é usada. */
enum class WidgetImageTarget { All, Square, Expanded, Compact }

/** Parte da imagem que fica visível quando ela é cortada para o formato do widget. */
enum class ImageFocus { Top, Center, Bottom }

/** Os três widgets, para decidir onde a imagem de fundo vale. */
enum class WidgetKind { Square, Expanded, Compact }

/**
 * Fundo com imagem nos widgets.
 * @param hasImage true quando há uma imagem escolhida e salva no app.
 * @param dimPercent escurecimento sobre a imagem (0–80), para o texto continuar legível.
 * @param blurPx desfoque em dp (0–20).
 */
data class WidgetImageSettings(
    val hasImage: Boolean = false,
    val dimPercent: Int = 45,
    val blurPx: Int = 6,
    val target: WidgetImageTarget = WidgetImageTarget.All,
    val focus: ImageFocus = ImageFocus.Center,
)

/** Escolhas de aparência feitas em Ajustes › Aparência. */
data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.Dark,
    val accent: AccentColor = AccentColor.Gold,
    val font: FontChoice = FontChoice.App,
    val animations: Boolean = true,
    val widgetStyle: WidgetStyle = WidgetStyle.Navy,
    val widgetAppCount: Int = 4,
    val widgetShowComparison: Boolean = true,
    val widgetImage: WidgetImageSettings = WidgetImageSettings(),
) {
    /**
     * Estilo efetivo de um widget: "Imagem" só vale onde o usuário escolheu e se
     * houver imagem salva; nos outros widgets cai para o marinho.
     */
    fun styleFor(kind: WidgetKind): WidgetStyle {
        if (widgetStyle != WidgetStyle.Image) return widgetStyle
        val applies = when (widgetImage.target) {
            WidgetImageTarget.All -> true
            WidgetImageTarget.Square -> kind == WidgetKind.Square
            WidgetImageTarget.Expanded -> kind == WidgetKind.Expanded
            WidgetImageTarget.Compact -> kind == WidgetKind.Compact
        }
        return if (applies && widgetImage.hasImage) WidgetStyle.Image else WidgetStyle.Navy
    }

    companion object {
        const val MIN_WIDGET_APPS = 3
        const val MAX_WIDGET_APPS = 4
        const val MAX_DIM = 80
        const val MAX_BLUR = 20
    }

    /** Corrige valores fora da faixa (por exemplo, vindos de uma versão antiga). */
    fun normalized(): AppearanceSettings = copy(
        widgetAppCount = widgetAppCount.coerceIn(MIN_WIDGET_APPS, MAX_WIDGET_APPS),
        widgetImage = widgetImage.copy(
            dimPercent = widgetImage.dimPercent.coerceIn(0, MAX_DIM),
            blurPx = widgetImage.blurPx.coerceIn(0, MAX_BLUR),
        ),
    )
}
