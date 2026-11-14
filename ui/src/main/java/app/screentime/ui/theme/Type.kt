package app.screentime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Tipografia com a fonte do sistema, ajustada em peso e espaçamento.
 * Números grandes (tempo de uso) usam peso leve e espaçamento negativo;
 * rótulos pequenos usam peso médio e leve espaçamento positivo.
 * Para usar uma fonte própria, coloque o .ttf em ui/src/main/res/font e troque [AppFont].
 */
private val AppFont = FontFamily.Default

val ScreenTimeTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Light, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-0.03).em),
    displayMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.025).em),
    displaySmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.01).em),
    headlineSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
    labelMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    labelSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.06.em),
)
