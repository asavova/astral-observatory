# Astral Observatory

An offline Java 21 application for calculating and displaying star positions using the HYG v4.1 catalogue.

## Run

```sh
bash scripts/run.sh
```

Open http://127.0.0.1:8766.

## Tests

```sh
bash scripts/test.sh
```

The app reads the original `data/hygdata_v41.csv` catalogue directly (119,626 entries, including the Sun). The star loader retains identifiers, J2000 coordinates, proper motion, distance, radial velocity, magnitude and colour index where available. Missing measurements are kept as missing. The sky chart uses approximate coordinate calculations.

Catalogue source and CC BY-SA 4.0 attribution: [data/ATTRIBUTION.md](data/ATTRIBUTION.md). Application code: MIT.
