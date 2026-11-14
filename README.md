# ScreenTime

App Android que mede o tempo de uso real do celular a partir dos eventos do sistema (`UsageStatsManager`/`UsageEvents`). Tudo fica no aparelho: o app não tem permissão de internet.

## Estado atual: etapas 1 a 9 concluídas

- Permissão de acesso ao uso com explicação e verificação automática.
- Coleta real a partir de `UsageEvents`, com 38 testes do cálculo (meia-noite, tela bloqueada, reinício, horário de verão).
- Painel de hoje com comparação com ontem até o mesmo horário e gráfico por hora.
- Lista de apps por dia, detalhe de cada app e histórico semanal.
- Histórico salvo em Room, sincronização a cada 30 min com WorkManager.
- Widgets compacto e expandido (Glance) com o horário da última atualização.
- Ajustes: bateria e HyperOS, histórico salvo, critérios do cálculo e diagnóstico.

## Como instalar pelo celular

O GitHub compila o app a cada envio de código (`.github/workflows/build.yml`).

1. Abra a aba **Actions** do repositório e espere a execução mais recente ficar verde.
2. Abra **Releases > ScreenTime: último build** e toque em `ScreenTime-debug.apk`.
3. Permita que o navegador instale apps desconhecidos, se o Android pedir.
4. Instale. As próximas versões instalam por cima, sem perder os dados, porque a assinatura é fixa (`app/debug.keystore`).

## Como compilar num computador (opcional)

Android Studio estável recente, SDK Platform 37, **File > Open** na pasta do projeto e **Run**.

## Versões usadas

| Item | Versão |
|---|---|
| Android Gradle Plugin | 9.5.1 |
| Gradle | 9.6.0 |
| Kotlin | 2.4.20 (Kotlin embutido do AGP 9) |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| Hilt (Dagger) | 2.60.1 |
| Room | 2.8.5 |
| WorkManager | 2.12.0 |
| Glance | 1.2.0 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

Todas ficam em `gradle/libs.versions.toml`.

## Módulos

| Módulo | Conteúdo |
|---|---|
| `:domain` | Modelos e regras em Kotlin puro |
| `:data` | Eventos do sistema, Room e repositório |
| `:permissions` | Verificação e pedido do acesso ao uso |
| `:ui` | Tema, tipografia, gráficos e componentes |
| `:presentation` | Telas Compose e ViewModels |
| `:widget` | Widgets Glance |
| `:workers` | Sincronização periódica com WorkManager |
| `:app` | Application, Activity e injeção de dependências |
