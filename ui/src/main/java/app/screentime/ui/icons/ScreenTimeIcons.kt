package app.screentime.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ícones de linha simples desenhados no próprio app (sem biblioteca de ícones).
 * A cor é aplicada pelo componente Icon (tint).
 */
object ScreenTimeIcons {

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private fun ImageVector.Builder.stroke(pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        )

    /** Relógio: aba "Hoje". */
    val Today: ImageVector by lazy {
        icon("Today") {
            stroke {
                moveTo(3f, 12f)
                arcToRelative(9f, 9f, 0f, true, true, 18f, 0f)
                arcToRelative(9f, 9f, 0f, true, true, -18f, 0f)
                close()
            }
            stroke {
                moveTo(12f, 7.5f)
                lineTo(12f, 12f)
                lineTo(15f, 14f)
            }
        }
    }

    /** Grade 2×2: aba "Apps". */
    val Apps: ImageVector by lazy {
        icon("Apps") {
            for ((x, y) in listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f)) {
                stroke {
                    moveTo(x + 1.5f, y)
                    horizontalLineToRelative(3f)
                    arcToRelative(1.5f, 1.5f, 0f, false, true, 1.5f, 1.5f)
                    verticalLineToRelative(3f)
                    arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, 1.5f)
                    horizontalLineToRelative(-3f)
                    arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, -1.5f)
                    verticalLineToRelative(-3f)
                    arcToRelative(1.5f, 1.5f, 0f, false, true, 1.5f, -1.5f)
                    close()
                }
            }
        }
    }

    /** Barras: aba "Semana". */
    val Week: ImageVector by lazy {
        icon("Week") {
            stroke {
                moveTo(5f, 20f); verticalLineTo(13f)
                moveTo(10f, 20f); verticalLineTo(6f)
                moveTo(15f, 20f); verticalLineTo(10f)
                moveTo(20f, 20f); verticalLineTo(15f)
            }
        }
    }

    /** Seta para voltar. */
    val Back: ImageVector by lazy {
        icon("Back") {
            stroke {
                moveTo(15f, 5f)
                lineTo(8f, 12f)
                lineTo(15f, 19f)
            }
        }
    }

    /** Seta para avançar. */
    val Forward: ImageVector by lazy {
        icon("Forward") {
            stroke {
                moveTo(9f, 5f)
                lineTo(16f, 12f)
                lineTo(9f, 19f)
            }
        }
    }
}
