# Astral Observatory

Offline night-sky simulator. Early Java 21 prototype with a small set of real star coordinates.

## Run

```sh
bash scripts/run.sh
```

Open http://127.0.0.1:8766. Requires Java 21.

## Tests

```sh
bash scripts/test.sh
```

The current build converts star coordinates to local altitude and azimuth and draws a basic sky map. Positions are approximate; this isn't suitable for telescope pointing yet. A separate offline subset of 5,070 HYG stars is in `data/hyg-bright-v41.json` (see `data/ATTRIBUTION.md`). It isn't wired into the app yet. Full catalogue support and the telescope model are future work.
