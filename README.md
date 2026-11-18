# TerraStyle 1.0.2 — Existing Overworld

This version is configured for the existing Paper world named `world`.
It does NOT create a separate `TerraWorld`, add commands, or teleport players.

## Install

1. Back up the entire server first.
2. Upload `TerraStyle-1.0.2.jar` to `plugins/`.
3. Open the server's `bukkit.yml`.
4. Under its existing top-level `worlds:` section, add:

   ```yaml
   worlds:
     world:
       generator: TerraStyle
   ```

   If `worlds:` already exists, add only the `world:` entry under it. Do not create a second `worlds:` key or replace the whole file.
5. Stop and restart the server.

## Important

- The world folder must be named exactly `world`. If your Overworld folder has a different name, change the `world` key in `bukkit.yml` and the world-name check in `TerraStylePlugin.java` to match.
- Only chunks generated after the generator is configured will use TerraStyle. It cannot reshape existing chunks.
- Deleting region/chunk files is destructive. Keep a backup; do not delete `level.dat`, playerdata, or other world data unless you intentionally want to reset those too.
- This is an original Terra-inspired generator, NOT the official Terra engine. It exposes the vanilla Overworld biome set available in the Paper 1.21.11 API (including underground cave biomes) and uses custom climate/region noise to distribute those biomes. It does not include Nether or End biomes because it is assigned only to the Overworld. Terrain shapes, coastlines, rivers, and surface materials are custom and differ from vanilla.
- This source project must be compiled by the included GitHub Actions workflow before installation. Check the Actions log; do not install the JAR if the build fails.
