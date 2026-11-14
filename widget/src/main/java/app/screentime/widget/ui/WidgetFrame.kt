package app.screentime.widget.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import app.screentime.domain.model.WidgetKind
import app.screentime.domain.model.WidgetStyle
import app.screentime.widget.R
import app.screentime.widget.RefreshAction
import app.screentime.widget.data.WidgetApp
import androidx.glance.layout.Alignment
import androidx.glance.text.Text
import androidx.compose.ui.unit.sp
import androidx.glance.text.FontWeight

/**
 * Moldura comum dos widgets: fundo do estilo escolhido (ou a imagem com escurecer
 * e desfoque) e toque que abre o app.
 */
@Composable
internal fun WidgetFrame(
    data: app.screentime.widget.data.WidgetData,
    kind: WidgetKind,
    corner: Dp,
    open: Intent,
    content: @Composable (WidgetPalette) -> Unit,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val settings = data.settings
    val style = settings.styleFor(kind)
    val palette = paletteFor(style, settings.accent)
    val path = data.backgroundPath
    val image = if (style == WidgetStyle.Image && path != null) {
        BackgroundRenderer.render(
            context = context,
            path = path,
            version = data.backgroundVersion,
            widthDp = size.width.value,
            heightDp = size.height.value,
            focus = settings.widgetImage.focus,
            dimPercent = settings.widgetImage.dimPercent,
            blurDp = settings.widgetImage.blurPx,
            cornerDp = corner.value,
        )
    } else {
        null
    }
    // Imagem que não abriu: volta para o fundo marinho, nunca fica sem fundo.
    val drawable = palette.background ?: if (image == null) R.drawable.widget_dark_bg else null

    var modifier = GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(corner)
    if (drawable != null) modifier = modifier.background(ImageProvider(drawable))
    modifier = modifier.clickable(actionStartActivity(open))

    Box(modifier = modifier) {
        if (image != null) {
            Image(
                provider = ImageProvider(image),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
        content(palette)
    }
}

/** Botão ↻: atualiza sem abrir o app. */
@Composable
internal fun RefreshButton(palette: WidgetPalette, size: Dp = 26.dp) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_refresh),
        contentDescription = LocalContext.current.getString(R.string.widget_refresh),
        colorFilter = ColorFilter.tint(palette.muted),
        modifier = GlanceModifier.size(size).padding(4.dp).clickable(actionRunCallback<RefreshAction>()),
    )
}

/** Ícone do app; sem ícone, a inicial do nome num quadrado. */
@Composable
internal fun AppIconTile(app: WidgetApp, palette: WidgetPalette, size: Dp) {
    val icon = app.icon
    if (icon != null) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(size).cornerRadius(size / 4),
        )
    } else {
        Box(
            modifier = GlanceModifier.size(size).background(palette.tile).cornerRadius(size / 4),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.name.take(1).uppercase(), style = palette.style(10.sp, FontWeight.Bold))
        }
    }
}

/** Selo da comparação: fundo colorido (verde = menos uso, coral = mais) e texto em negrito, legível sobre qualquer fundo. */
@Composable
internal fun ComparisonPill(percent: Int, palette: WidgetPalette, long: Boolean, time: String) {
    Box(
        modifier = GlanceModifier
            .background(if (percent <= 0) palette.betterBg else palette.worseBg)
            .cornerRadius(8.dp)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(comparisonText(percent, long, time), style = palette.style(11.sp, FontWeight.Bold), maxLines = 1)
    }
}
