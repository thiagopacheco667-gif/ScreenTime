package app.screentime.domain.repository

/**
 * Imagem de fundo dos widgets, guardada como uma cópia reduzida dentro do app.
 * O app só lê a foto que o usuário escolheu no seletor do sistema.
 */
interface WidgetBackgroundStore {
    /**
     * Copia a imagem do [uri] (content:// do seletor de fotos) para o app.
     * @return true se a cópia deu certo.
     */
    suspend fun saveFrom(uri: String): Boolean

    /** Apaga a cópia salva. */
    suspend fun clear()

    /** Caminho da cópia salva, ou nulo se não houver. */
    fun imagePath(): String?

    /** Muda a cada imagem nova (para recarregar prévias). */
    fun version(): Long
}
