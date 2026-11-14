package app.screentime.presentation.appearance

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.color.WidgetImageColors
import app.screentime.domain.model.AppearanceSettings
import app.screentime.domain.model.WidgetColorSource
import app.screentime.domain.model.ImageFocus
import app.screentime.domain.model.WidgetImageTarget
import app.screentime.domain.model.WidgetStyle
import app.screentime.presentation.R
import app.screentime.ui.theme.ScreenTimeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Seção "Widgets" de Ajustes › Aparência. */
@Composable
internal fun WidgetsSection(viewModel: AppearanceViewModel, s: AppearanceSettings) {
    val image by viewModel.image.collectAsStateWithLifecycle()
    val busy by viewModel.imageBusy.collectAsStateWithLifecycle()
    val error by viewModel.imageError.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickImage(uri)
    }
    val c = ScreenTimeTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.appearance_widgets).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = c.card,
            border = BorderStroke(1.dp, c.cardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.appearance_widget_style), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WidgetStyle.entries.forEach { style ->
                        StyleOption(style, selected = s.widgetStyle == style, modifier = Modifier.weight(1f)) {
                            if (style == WidgetStyle.Image && image.path == null) {
                                viewModel.setWidgetStyle(style)
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                viewModel.setWidgetStyle(style)
                            }
                        }
                    }
                }

                if (s.widgetStyle == WidgetStyle.Image) {
                    ImagePanel(
                        s = s,
                        image = image,
                        busy = busy,
                        error = error,
                        onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        viewModel = viewModel,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.appearance_widget_apps), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.appearance_widget_apps_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 4).forEach { n ->
                            val on = s.widgetAppCount == n
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (on) MaterialTheme.colorScheme.primary else c.inset)
                                    .selectable(selected = on, role = Role.RadioButton) { viewModel.setWidgetAppCount(n) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$n",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = s.widgetShowComparison, role = Role.Switch, onValueChange = viewModel::setWidgetComparison),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.appearance_widget_cmp), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.appearance_widget_cmp_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = s.widgetShowComparison,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.appearance_widget_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StyleOption(style: WidgetStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else ScreenTimeTheme.colors.cardBorder,
        label = "style-ring",
    )
    val (fill, text, label) = when (style) {
        WidgetStyle.Navy -> Triple(Brush.linearGradient(listOf(Color(0xFF262D52), Color(0xFF12172E))), Color(0xFFF2F4FF), R.string.widget_style_navy)
        WidgetStyle.Light -> Triple(Brush.linearGradient(listOf(Color.White, Color(0xFFF1F2F7))), Color(0xFF15181D), R.string.widget_style_light)
        WidgetStyle.Transparent -> Triple(Brush.linearGradient(listOf(Color(0x660E1120), Color(0x990E1120))), Color.White, R.string.widget_style_transparent)
        WidgetStyle.Image -> Triple(Brush.verticalGradient(listOf(Color(0xFF8EA3B8), Color(0xFFC9B9A6), Color(0xFF5E6B5A))), Color.White, R.string.widget_style_image)
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(fill)
            .border(2.dp, ring, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = text, maxLines = 1)
    }
}

@Composable
private fun ImagePanel(
    s: AppearanceSettings,
    image: BackgroundImage,
    busy: Boolean,
    error: Boolean,
    onPick: () -> Unit,
    viewModel: AppearanceViewModel,
) {
    val c = ScreenTimeTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.inset)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.appearance_image_preview),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ImagePreview(image, s)

        if (error) {
            Text(stringResource(R.string.appearance_image_error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPick,
                enabled = !busy,
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    stringResource(
                        when {
                            busy -> R.string.appearance_image_busy
                            image.path == null -> R.string.appearance_image_pick
                            else -> R.string.appearance_image_change
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (image.path != null) {
                OutlinedButton(
                    onClick = viewModel::removeImage,
                    modifier = Modifier.heightIn(min = 44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(R.string.appearance_image_remove), style = MaterialTheme.typography.labelLarge) }
            }
        }

        Labeled(stringResource(R.string.appearance_image_colors)) {
            Chips(
                options = listOf(
                    WidgetColorSource.Image to stringResource(R.string.appearance_colors_image),
                    WidgetColorSource.Theme to stringResource(R.string.appearance_colors_theme),
                ),
                selected = s.widgetImage.colors,
                onSelect = viewModel::setImageColors,
            )
            val p = image.palette
            if (p != null && s.widgetImage.colors == WidgetColorSource.Image) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(p.dark, p.mid, p.vivid, p.light).forEach { argb ->
                        Box(Modifier.size(22.dp).clip(CircleShape).background(Color(argb)).border(1.dp, Color(0x33FFFFFF), CircleShape))
                    }
                    Text(
                        stringResource(R.string.appearance_colors_found),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Labeled(stringResource(R.string.appearance_image_focus)) {
            Chips(
                options = listOf(
                    ImageFocus.Top to stringResource(R.string.appearance_focus_top),
                    ImageFocus.Center to stringResource(R.string.appearance_focus_center),
                    ImageFocus.Bottom to stringResource(R.string.appearance_focus_bottom),
                ),
                selected = s.widgetImage.focus,
                onSelect = viewModel::setImageFocus,
            )
        }

        SliderRow(
            title = stringResource(R.string.appearance_image_dim),
            value = s.widgetImage.dimPercent,
            max = AppearanceSettings.MAX_DIM,
            step = 5,
            valueText = "${s.widgetImage.dimPercent}%",
            hint = stringResource(R.string.appearance_image_dim_hint),
            onChange = viewModel::setImageDim,
            onDone = viewModel::refreshWidgets,
        )
        SliderRow(
            title = stringResource(R.string.appearance_image_blur),
            value = s.widgetImage.blurPx,
            max = AppearanceSettings.MAX_BLUR,
            step = 1,
            valueText = if (s.widgetImage.blurPx == 0) stringResource(R.string.appearance_image_blur_none) else "${s.widgetImage.blurPx} px",
            hint = null,
            onChange = viewModel::setImageBlur,
            onDone = viewModel::refreshWidgets,
        )

        Labeled(stringResource(R.string.appearance_image_target)) {
            Chips(
                options = listOf(
                    WidgetImageTarget.All to stringResource(R.string.appearance_target_all),
                    WidgetImageTarget.Square to stringResource(R.string.appearance_target_square),
                    WidgetImageTarget.Expanded to stringResource(R.string.appearance_target_expanded),
                    WidgetImageTarget.Compact to stringResource(R.string.appearance_target_compact),
                ),
                selected = s.widgetImage.target,
                onSelect = viewModel::setImageTarget,
            )
        }
    }
}

/** Prévia: a mesma foto, com o escurecimento e o desfoque escolhidos. */
@Composable
private fun ImagePreview(image: BackgroundImage, s: AppearanceSettings) {
    val imageColors = image.palette
        ?.takeIf { s.widgetImage.colors == WidgetColorSource.Image }
        ?.let { WidgetImageColors.from(it, s.widgetImage.dimPercent) }
    val accent = imageColors?.let { Color(it.accent) } ?: MaterialTheme.colorScheme.primary
    val barBrush = imageColors?.let { Brush.verticalGradient(listOf(Color(it.barTop), Color(it.barBottom))) }
        ?: Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.85f)))
    val bitmap by produceState<ImageBitmap?>(initialValue = null, image.path, image.version) {
        val path = image.path
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            }
        }
    }
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(Color(0xFF12172E))
            .border(1.dp, Color(0x24FFFFFF), shape),
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = when (s.widgetImage.focus) {
                    ImageFocus.Top -> Alignment.TopCenter
                    ImageFocus.Center -> Alignment.Center
                    ImageFocus.Bottom -> Alignment.BottomCenter
                },
                // O desfoque da prévia existe a partir do Android 12; o widget é desfocado em qualquer versão.
                modifier = Modifier.fillMaxSize().blur(s.widgetImage.blurPx.dp),
            )
            val colors = image.palette
                ?.takeIf { s.widgetImage.colors == WidgetColorSource.Image }
                ?.let { WidgetImageColors.from(it, s.widgetImage.dimPercent) }
            if (colors != null) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(
                                Color(colors.overlayStart).copy(alpha = colors.overlayAlpha),
                                Color(colors.overlayEnd).copy(alpha = colors.overlayAlpha),
                            ),
                        ),
                    ),
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF080A14).copy(alpha = s.widgetImage.dimPercent / 100f)))
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.appearance_preview_today), style = MaterialTheme.typography.labelSmall, color = accent)
            Text(
                if (current == null) stringResource(R.string.appearance_image_none) else "1 h 23 min",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            if (current != null) {
                Row(Modifier.fillMaxWidth().height(28.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(0.95f, 0.85f, 0.25f, 0.1f, 0.55f, 0.7f, 0.75f, 0.5f, 0.2f, 0.1f, 0.05f, 0.3f).forEach { h ->
                        Box(Modifier.weight(1f).fillMaxHeight(h).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(barBrush))
                    }
                }
            }
        }
        if (current != null) {
            Surface(
                shape = CircleShape,
                color = Color(0x73000000),
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            ) {
                Text(
                    stringResource(R.string.appearance_preview_example),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun Labeled(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun <T> Chips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = ScreenTimeTheme.colors
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) MaterialTheme.colorScheme.primary else c.card)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Int,
    max: Int,
    step: Int,
    valueText: String,
    hint: String?,
    onChange: (Int) -> Unit,
    onDone: () -> Unit,
) {
    Column {
        Row {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            onValueChangeFinished = onDone,
            valueRange = 0f..max.toFloat(),
            steps = (max / step) - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
