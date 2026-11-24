# Star catalogue attribution

File: `hyg-bright-v41.json`

- Original dataset: HYG Star Database v4.1, David Nash / Astronexus.
- Original source: https://github.com/astronexus/HYG-Database/blob/3bf37f4b2d5460e1278286320d1d62fab9b493c1/hyg/CURRENT/hygdata_v41.csv
- Filtered data source: RTF22/Orrery, `src/data/stars/hyg.json`: https://github.com/RTF22/Orrery/blob/master/src/data/stars/hyg.json
- License for catalogue data: Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0): https://creativecommons.org/licenses/by-sa/4.0/

The upstream Orrery project filtered HYG v4.1 to 5,070 stars of apparent magnitude <= 6.0, removed the Sun, and kept only RA (degrees), declination (degrees), apparent magnitude and B−V colour index. Positions were rounded to three decimal places, and missing colour indices were replaced with 0.0.

The copy here is unchanged from that filtered JSON. The catalogue is stored locally for offline use. It is separate from the project's MIT-licensed application code; derivative catalogue data remains subject to CC BY-SA 4.0.

The current Java prototype still loads `bright-stars-sample.csv`. Loading and rendering the larger HYG subset is future work.
