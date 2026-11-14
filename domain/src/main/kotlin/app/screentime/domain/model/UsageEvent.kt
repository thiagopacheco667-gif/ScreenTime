package app.screentime.domain.model

/**
 * Tipos de evento do sistema que importam para o cálculo.
 * Os demais tipos do UsageEvents (configuração, notificações, etc.) são descartados na leitura.
 */
enum class UsageEventType {
    /** Uma tela (activity) do app ficou visível e em foco. */
    ActivityResumed,

    /** A tela perdeu o foco. */
    ActivityPaused,

    /** A tela deixou de ser visível (Android 10+). */
    ActivityStopped,

    /** Tela do celular ligada (Android 9+). */
    ScreenInteractive,

    /** Tela do celular desligada (Android 9+). */
    ScreenNonInteractive,

    /** Tela de bloqueio exibida (Android 9+). */
    KeyguardShown,

    /** Celular desbloqueado (Android 9+). */
    KeyguardHidden,

    /** Celular desligando (Android 10+). */
    DeviceShutdown,

    /** Celular ligou (Android 10+). */
    DeviceStartup,
}

/**
 * Um evento bruto, como o sistema registrou.
 *
 * @param timestamp milissegundos desde 1970 (relógio do sistema).
 * @param className nome da activity; nulo para eventos de tela e de aparelho.
 */
data class UsageEvent(
    val timestamp: Long,
    val packageName: String,
    val className: String?,
    val type: UsageEventType,
)
