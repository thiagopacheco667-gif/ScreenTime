# ScreenTime

App Android que mede o tempo de uso real do celular a partir dos eventos do sistema (`UsageStatsManager`/`UsageEvents`). Tudo fica no aparelho: o app não tem permissão de internet.

## Estado atual: etapa 2 de 8

Estrutura de módulos, Gradle, manifesto, tema claro/escuro e uma tela temporária que confirma que o projeto compila.

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

## Verificação desta etapa

- O Gradle Sync termina sem erros.
- O app instala e abre mostrando "ScreenTime" e a mensagem de estrutura pronta.
- Com o modo escuro do celular ligado, o fundo fica grafite.

Se a execução no Actions ficar vermelha, o erro aparece no log da etapa que falhou.

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
