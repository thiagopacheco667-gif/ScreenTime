package app.screentime.domain.engine

/**
 * Conta quantas vezes cada app foi aberto num dia ("aberto N×").
 *
 * Uma volta ao mesmo app conta como a MESMA abertura quando, ao mesmo tempo:
 * 1. passaram no máximo [returnWindowMillis] desde que ele saiu da tela; e
 * 2. nesse intervalo nenhum outro app de verdade ficou na tela por pelo menos
 *    [interruptMillis]. Telas de sistema que outros apps abrem por cima
 *    (compartilhar, permissões, seletor de fotos e arquivos, login do Google)
 *    não interrompem, porque o usuário não "saiu" do app.
 *
 * Exemplos: compartilhar uma foto e voltar, a tela apagar e acender em seguida,
 * ou espiar os apps recentes e voltar — tudo isso continua sendo uma abertura.
 * Ir para outro app (ou ficar na tela inicial) e voltar depois é uma abertura nova.
 */
class OpenCounter(
    private val returnWindowMillis: Long = DEFAULT_RETURN_WINDOW_MILLIS,
    private val interruptMillis: Long = DEFAULT_INTERRUPT_MILLIS,
    private val transientPackages: Set<String> = TRANSIENT_PACKAGES,
) {

    /** @param sessions sessões de TODOS os pacotes (inclusive excluídos), já cortadas no dia. */
    fun count(sessions: List<Session>): Map<String, Int> {
        val opens = HashMap<String, Int>()
        val lastEnd = HashMap<String, Long>()
        val interrupted = HashMap<String, Boolean>()

        for (s in sessions.sortedBy { it.start }) {
            val pkg = s.packageName
            val previousEnd = lastEnd[pkg]
            val sameOpen = previousEnd != null &&
                s.start - previousEnd <= returnWindowMillis &&
                interrupted[pkg] != true
            if (!sameOpen) opens[pkg] = (opens[pkg] ?: 0) + 1

            // Este app na tela interrompe os outros, se não for uma tela de sistema passageira.
            if (pkg !in transientPackages && s.durationMillis >= interruptMillis) {
                for (other in lastEnd.keys) if (other != pkg) interrupted[other] = true
            }
            lastEnd[pkg] = s.end
            interrupted[pkg] = false
        }
        return opens
    }

    companion object {
        const val DEFAULT_RETURN_WINDOW_MILLIS = 30_000L
        const val DEFAULT_INTERRUPT_MILLIS = 2_000L

        /** Telas que aparecem por cima de outros apps sem o usuário sair deles. */
        val TRANSIENT_PACKAGES: Set<String> = setOf(
            "android",
            "com.android.systemui",
            "com.android.intentresolver",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.providers.media.module",
            "com.google.android.providers.media.module",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.google.android.gms",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.miui.securitycenter",
        )
    }
}
