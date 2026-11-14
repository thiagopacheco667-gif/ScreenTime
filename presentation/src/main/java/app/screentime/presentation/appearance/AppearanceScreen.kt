package app.screentime.presentation.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.screentime.domain.model.AccentColor
import app.screentime.domain.model.FontChoice
import app.screentime.domain.model.ThemeMode
import app.screentime.presentation.R
import app.screentime.ui.icons.ScreenTimeIcons
import app.screentime.ui.theme.ManropeFamily
import app.screentime.ui.theme.ScreenTimeTheme

@Composable
fun AppearanceRoute(
    viewModel: AppearanceViewModel,
    onBack: () -> Unit,
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(ScreenTimeIcons.Back, contentDescription = stringResource(R.string.nav_back))
            }
            Column {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.headlineSmall)
            }
        }

        Section(stringResource(R.string.appearance_preview)) { PreviewCard() }

        Section(stringResource(R.string.appearance_theme)) {
            Segmented(
                options = listOf(
                    ThemeMode.System to stringResource(R.string.appearance_theme_system),
                    ThemeMode.Light to stringResource(R.string.appearance_theme_light),
                    ThemeMode.Dark to stringResource(R.string.appearance_theme_dark),
                ),
                selected = s.themeMode,
                onSelect = viewModel::setTheme,
            )
        }

        Section(stringResource(R.string.appearance_accent)) {
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccentColor.entries.forEach { option ->
                    AccentSwatch(
                        option = option,
                        selected = option == s.accent,
                        onClick = { viewModel.setAccent(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Section(stringResource(R.string.appearance_font)) {
            Card {
                Column(Modifier.selectableGroup()) {
                    FontOption(
                        title = stringResource(R.string.appearance_font_app),
                        subtitle = stringResource(R.string.appearance_font_app_sub),
                        family = ManropeFamily,
                        selected = s.font == FontChoice.App,
                        onClick = { viewModel.setFont(FontChoice.App) },
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(ScreenTimeTheme.colors.divider))
                    FontOption(
                        title = stringResource(R.string.appearance_font_system),
                        subtitle = stringResource(R.string.appearance_font_system_sub),
                        family = FontFamily.Default,
                        selected = s.font == FontChoice.System,
                        onClick = { viewModel.setFont(FontChoice.System) },
                    )
                }
            }
        }

        Section(stringResource(R.string.appearance_motion)) {
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = s.animations, role = Role.Switch, onValueChange = viewModel::setAnimations)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.appearance_animations), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.appearance_animations_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = s.animations,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.appearance_widgets_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ScreenTimeTheme.colors.card,
        border = BorderStroke(1.dp, ScreenTimeTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

/** Prévia ao vivo: muda junto com tema, cor e fonte (números de exemplo, sinalizados). */
@Composable
private fun PreviewCard() {
    val c = ScreenTimeTheme.colors
    val heights = listOf(0.98f, 0.92f, 0.24f, 0.06f, 0.5f, 0.72f, 0.76f, 0.57f, 0.17f, 0.07f, 0.04f, 0.28f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
            .border(1.dp, c.heroBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.appearance_preview_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Surface(shape = CircleShape, color = c.inset) {
                Text(
                    stringResource(R.string.appearance_preview_example),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text("1 h 23 min", style = MaterialTheme.typography.displaySmall)
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            heights.forEachIndexed { i, h ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(h)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (i == 6) MaterialTheme.colorScheme.primary else c.chartBar),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.track)) {
            Box(Modifier.fillMaxWidth(0.72f).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = ScreenTimeTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, c.cardBorder, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            val bg by animateColorAsState(if (on) c.navIndicator else Color.Transparent, label = "seg-bg")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(value) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(option: AccentColor, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = ScreenTimeTheme.colors
    val color = if (c.isDark) option.toUi().onDark else option.toUi().onLight
    val ring by animateColorAsState(if (selected) color else c.cardBorder, label = "swatch-ring")
    val dot by animateDpAsState(if (selected) 30.dp else 26.dp, label = "swatch-dot")
    Column(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(2.dp, ring, RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(dot).clip(CircleShape).background(color))
        Text(stringResource(option.labelRes()), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FontOption(title: String, subtitle: String, family: FontFamily, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(20.dp).border(2.dp, if (selected) accent else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontFamily = family))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun AccentColor.labelRes(): Int = when (this) {
    AccentColor.Gold -> R.string.accent_gold
    AccentColor.Blue -> R.string.accent_blue
    AccentColor.Green -> R.string.accent_green
    AccentColor.Coral -> R.string.accent_coral
    AccentColor.Lilac -> R.string.accent_lilac
}
