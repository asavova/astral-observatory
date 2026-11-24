# Astral Observatory

Offline night-sky prototype in Java 21.

## Run

```sh
bash scripts/run.sh
```

Open http://127.0.0.1:8766.

## Tests

```sh
bash scripts/test.sh
```

Loads 5,070 HYG bright stars from `data/hyg-bright-v41.json` and plots their approximate positions above the horizon for an observer's location and time. Data credit and license: `data/ATTRIBUTION.md`.

This is a basic sky chart. Telescope pointing, zoom and tracking are not implemented yet.
