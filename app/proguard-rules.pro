# Regras do R8 para o build release.
# Room, Hilt, WorkManager e Glance já trazem suas próprias regras.
# Mantém os nomes das classes de rota da navegação tipada (kotlinx.serialization).
-keepclassmembers @kotlinx.serialization.Serializable class app.screentime.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
