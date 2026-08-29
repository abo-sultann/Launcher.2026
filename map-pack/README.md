# Saudi offline map pack builder

This branch-only builder creates a standalone Saudi Arabia Mapsforge pack without changing the Launcher APK.

It performs the following checks before publishing an artifact:

1. Validates the current Geofabrik GCC source checksum.
2. Extracts OpenStreetMap relation `307584` (Saudi Arabia) using Osmium.
3. Adds complete land/sea polygons using the official Mapsforge creation approach.
4. Generates an Arabic-first Mapsforge v5 map with Mapsforge writer `0.30.0`.
5. Generates a POI v4 database with all tags, Arabic normalization, geo tags, R-Tree, and FTS5 trigram search.
6. Opens and reads 63 city-area tiles using Mapsforge reader `0.25.0`, matching the current Launcher dependency.
7. Runs SQLite integrity, Arabic-content, normalization, and full-text-search checks.

The output is uploaded only as a temporary GitHub Actions artifact. It is not merged into the application or released as an APK.
