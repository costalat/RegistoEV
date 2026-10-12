#!/usr/bin/env bun
/**
 * refresh-fallback.mjs — snapshot the live «query» sheet into query-fallback.csv
 *
 * The dashboard reads the Google Sheet live via the public gviz endpoint.
 * This script keeps a local copy so the page still renders offline / if the
 * sheet ever becomes private. Run it whenever you want to sync the snapshot.
 *
 *   bun run refresh-fallback.mjs
 */
const SHEET_ID = "1s7cHd4g2F9RC0WwRGlRtm4eqAoxXaXPiKmoqwK2J22g";
const SHEET    = "query";
const OUT      = new URL("./query-fallback.csv", import.meta.url);

const url = `https://docs.google.com/spreadsheets/d/${SHEET_ID}/gviz/tq?tqx=out:csv&sheet=${encodeURIComponent(SHEET)}`;
const res = await fetch(url);
if (!res.ok) { console.error("fetch failed:", res.status); process.exit(1); }
const csv = await res.text();
if (!/Matricula_Civil/i.test(csv)) { console.error("unexpected payload — sheet public?"); process.exit(1); }
await Bun.write(OUT, csv);
const lines = csv.trim().split("\n").length - 1;
console.log(`saved ${OUT.pathname} — ${lines} data rows`);
