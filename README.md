# Registo EV

App Android (Kotlin · Jetpack Compose · Room) para registo de movimentos e
carregamentos de veículos elétricos, com sincronização automática para uma
folha Google (`query`) que agrega os dados de todas as instalações.

## Painel web

Dashboard de indicadores que lê a folha `query` em direto:

**https://costalat.github.io/RegistoEV/**

Código em [`docs/`](docs/) — ver [`docs/README.md`](docs/README.md).

## Aplicação Android

O projeto Gradle está na raiz (`app/`). Principais ecrãs:

- `HomeScreen` — registo e resumo
- `AddChargeScreen` — novo movimento / carregamento
- `DashboardScreen` — estatísticas nativas
- `EditHistoryScreen` — gestão de histórico
- `BackupScreen` — exportação CSV / JSON / PDF
- `SettingsScreen` — veículo, condutor, unidade

Sincronização para a folha: `sync/CloudSyncManager.kt` + `sync/SyncWorker.kt`
(POST JSON para um Google Apps Script).
