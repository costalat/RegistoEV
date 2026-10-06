# Painel de Frota (web) — docs/

Dashboard web que lê **em direto** a folha `query` do Google Sheets e a
apresenta como um cockpit de indicadores. Serve-se diretamente pelo
**GitHub Pages** a partir desta pasta.

> URL esperado: `https://costalat.github.io/RegistoEV/`

## Como publicar (GitHub Pages)

1. No GitHub: **Settings → Pages**
2. **Source:** *Deploy from a branch*
3. **Branch:** `test-branch-3` · **Folder:** `/docs`
4. Guardar. Ao fim de ~1 min fica em `https://costalat.github.io/RegistoEV/`

O ficheiro `.nojekyll` já está incluído para o Pages servir os ficheiros como estão.

## Ficheiros

| Ficheiro | Papel |
|---|---|
| `index.html` | O dashboard completo (HTML + CSS + JS + ECharts via CDN) |
| `query-fallback.csv` | Cópia local dos dados (usada se a folha estiver inacessível) |
| `refresh-fallback.mjs` | Atualiza a cópia local a partir da folha |
| `.nojekyll` | Impede o Jekyll de processar a pasta |

## Fonte de dados

- **Folha:** `query` do spreadsheet `1s7cHd4g2F9RC0WwRGlRtm4eqAoxXaXPiKmoqwK2J22g`
- Lida com o endpoint público **gviz** do Google (`/gviz/tq?tqx=out:json&sheet=query`)
- O browser injeta um `<script>` e define o shim `google.visualization.Query.setResponse`
  — contorna CORS **sem servidor, sem chaves de API**
- Atualiza a cada carregamento da página

⚠️ **A folha tem de continuar com leitura pública.** Se passar a privada, o
dashboard deixa de a conseguir ler (nesse caso usa-se a cópia local, ou um proxy
com API key).

## Esquema da folha `query`

| Coluna | Significado |
|---|---|
| `Matricula_Civil` | Viatura (`plate`/`matricula`) |
| `Origem` / `Destino` | Movimento (`origin`/`destination`) |
| `Observações` | Notas |
| `kWh - VOLTE.E (Exército)` | Carregamento na rede Exército (`chargeType = Rede Exército`) |
| `kWh - MOBI.E (Frota)` | Carregamento na rede pública (`chargeType = Rede MOBI.E`) |
| `Código Posto Carregamento` | Posto (`codPosto`) |
| `Litros` | Combustível (`liters`/`litros`) |
| `Odómetro` | Odómetro (`odometer`) |
| `Data` | Data (`date`) |
| `Condutor` | Condutor (`driver`/`condutor`) |
| `Column 1` | ID/sequência |

Cada instalação da app Android sincroniza os seus registos para esta folha
(`CloudSyncManager` → Google Apps Script `/exec`), pelo que a folha é a
**agregação de todas as instalações**.

### Carregamentos vs Movimentos

- **Carregamento** — linha com kWh (numa das duas colunas de rede)
- **Movimento** — viagem sem carregamento (só odómetro/origem/destino)

## O que o dashboard mostra

- **6 KPIs** — Distância · Energia · Consumo · Carregamentos · Movimentos · Viaturas
- **3 medidores** — Eletrificação · Utilização · Intensidade energética
- **Combo** — energia empilhada (MOBI.E + VOLTE.E) e viagens por mês
- **Radar** — perfil normalizado por viatura
- **Energia por rede** — donut MOBI.E vs VOLTE.E
- **Bubble** — postos: sessões × energia, tamanho = kWh/sessão
- **Mapa de calor** — posto × mês (energia)
- **Tabelas** — Top postos · Por viatura · Por mês

## Desenvolvimento local

```bash
cd docs
python3 -m http.server 8899
# abrir http://localhost:8899/index.html
```

Atualizar a cópia offline:

```bash
bun run refresh-fallback.mjs
```
