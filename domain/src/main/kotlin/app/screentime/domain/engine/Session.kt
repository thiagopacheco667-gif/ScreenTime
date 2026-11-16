package app.screentime.domain.engine

/**
 * Intervalo [start, end) em que um app esteve na tela, em milissegundos.
 *
 * @param focused verdadeiro quando o app estava em foco (o usuário mexendo nele).
 *   Falso quando ele só continuava visível sem foco: vídeo em janela flutuante
 *   (picture-in-picture) ou uma janela de outro app aberta por cima.
 */
data class Session(
    val packageName: String,
    val start: Long,
    val end: Long,
    val focused: Boolean = true,
) {
    val durationMillis: Long get() = end - start
}
