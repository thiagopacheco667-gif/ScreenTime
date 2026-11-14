package app.screentime.domain.engine

/** Intervalo [start, end) em que um app esteve em primeiro plano, em milissegundos. */
data class Session(
    val packageName: String,
    val start: Long,
    val end: Long,
) {
    val durationMillis: Long get() = end - start
}
