# Astral Observatory

Offline Java 21 starter for a telescope simulator. For now, it draws a basic sky chart using real HYG star positions.

## Run

```sh
bash scripts/run.sh
```

Open http://127.0.0.1:8766.

## Tests

```sh
bash scripts/test.sh
```

The local catalogue contains 119,625 stars (`data/hyg-full-v41.tsv`). Source and license details are in `data/ATTRIBUTION.md`.

Telescope pointing, zoom, tracking and optics are future work.
