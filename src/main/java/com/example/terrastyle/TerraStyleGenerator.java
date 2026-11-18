package com.example.terrastyle;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.Random;

/**
 * TerraStyle's custom Overworld terrain. This is an original generator, not the
 * official Terra engine. The biome provider exposes every vanilla Overworld
 * biome available in the Paper 1.21.11 API, and the climate/region maps select
 * among them across the world.
 */
public final class TerraStyleGenerator extends ChunkGenerator {
    private static final int SEA_LEVEL = 63;

    // All vanilla Overworld biomes in the 1.21.11-era biome set, including
    // cave biomes. Nether and End biomes are intentionally excluded because
    // this generator is only assigned to the Overworld.
    private static final List<Biome> OVERWORLD_BIOMES = List.of(
            Biome.BADLANDS, Biome.BAMBOO_JUNGLE, Biome.BEACH, Biome.BIRCH_FOREST,
            Biome.CHERRY_GROVE, Biome.COLD_OCEAN, Biome.DARK_FOREST,
            Biome.DEEP_COLD_OCEAN, Biome.DEEP_DARK, Biome.DEEP_FROZEN_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.DESERT,
            Biome.DRIPSTONE_CAVES, Biome.ERODED_BADLANDS, Biome.FLOWER_FOREST,
            Biome.FOREST, Biome.FROZEN_OCEAN, Biome.FROZEN_PEAKS,
            Biome.FROZEN_RIVER, Biome.GROVE, Biome.ICE_SPIKES,
            Biome.JAGGED_PEAKS, Biome.JUNGLE, Biome.LUKEWARM_OCEAN,
            Biome.LUSH_CAVES, Biome.MANGROVE_SWAMP, Biome.MEADOW,
            Biome.MUSHROOM_FIELDS, Biome.OCEAN, Biome.OLD_GROWTH_BIRCH_FOREST,
            Biome.OLD_GROWTH_PINE_TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA,
            Biome.PALE_GARDEN, Biome.PLAINS, Biome.RIVER, Biome.SAVANNA,
            Biome.SAVANNA_PLATEAU, Biome.SNOWY_BEACH, Biome.SNOWY_PLAINS,
            Biome.SNOWY_SLOPES, Biome.SNOWY_TAIGA, Biome.SPARSE_JUNGLE,
            Biome.STONY_PEAKS, Biome.STONY_SHORE, Biome.SUNFLOWER_PLAINS,
            Biome.SWAMP, Biome.TAIGA, Biome.WARM_OCEAN,
            Biome.WINDSWEPT_FOREST, Biome.WINDSWEPT_GRAVELLY_HILLS,
            Biome.WINDSWEPT_HILLS, Biome.WINDSWEPT_SAVANNA,
            Biome.WOODED_BADLANDS
    );

    private final BiomeProvider biomeProvider = new TerraBiomeProvider();

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return biomeProvider;
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return true; }
    @Override public boolean shouldGenerateDecorations() { return true; }
    @Override public boolean shouldGenerateMobs() { return true; }
    @Override public boolean shouldGenerateStructures() { return true; }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        int minY = data.getMinHeight();
        int maxY = data.getMaxHeight();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = chunkX * 16 + lx;
                int z = chunkZ * 16 + lz;
                int height = Math.max(minY + 8, Math.min(maxY - 8, terrainHeight(x, z)));
                Biome biome = surfaceBiomeAt(x, z, height);

                Material top = surfaceFor(biome, height);
                Material filler = fillerFor(biome);
                for (int y = minY + 1; y <= height; y++) {
                    data.setBlock(lx, y, lz, Material.STONE);
                }
                data.setBlock(lx, height, lz, top);
                for (int depth = 1; depth <= 5 && height - depth > minY; depth++) {
                    data.setBlock(lx, height - depth, lz, filler);
                }

                // Water fills oceans and low river channels. The near-shore
                // height band is intentionally broad enough for sandy beaches.
                if (height < SEA_LEVEL) {
                    for (int y = height + 1; y <= Math.min(SEA_LEVEL, maxY - 1); y++) {
                        data.setBlock(lx, y, lz, Material.WATER);
                    }
                }

                data.setBlock(lx, minY, lz, Material.BEDROCK);
                for (int layer = 1; layer <= 4; layer++) {
                    if (hash01(x, minY + layer, z) < (0.78 - layer * 0.17)) {
                        data.setBlock(lx, minY + layer, lz, Material.BEDROCK);
                    }
                }
            }
        }
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        // Surface materials are placed directly during custom terrain creation.
    }

    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, org.bukkit.HeightMap heightMap) {
        return terrainHeight(x, z) + 1;
    }

    private static int terrainHeight(int x, int z) {
        double continent = valueNoise(x * 0.00125, z * 0.00125, 918273) * 30.0;
        double hills = valueNoise(x * 0.0065, z * 0.0065, 481516) * 17.0;
        double detail = valueNoise(x * 0.028, z * 0.028, 192837) * 4.0;
        double mountainMask = smoothstep(0.30, 0.78,
                (valueNoise(x * 0.0022, z * 0.0022, 564738) + 1.0) * 0.5);
        double ridges = 1.0 - Math.abs(valueNoise(x * 0.0038, z * 0.0038, 837261));
        double mountains = mountainMask * ridges * 118.0;

        // Broad lowlands and gently sloping coasts, with narrow winding rivers.
        double base = 77.0 + continent + hills + detail + mountains;
        double riverNoise = Math.abs(valueNoise(x * 0.0042, z * 0.0042, 771122));
        double river = 1.0 - smoothstep(0.025, 0.095, riverNoise);
        base -= river * Math.max(0.0, base - 57.0) * 0.72;
        return (int) Math.round(base);
    }

    private static Biome surfaceBiomeAt(int x, int z, int height) {
        double temperature = (valueNoise(x * 0.0013, z * 0.0013, 334455) + 1.0) * 0.5;
        double moisture = (valueNoise(x * 0.0017, z * 0.0017, 778899) + 1.0) * 0.5;
        double region = (valueNoise(x * 0.00072, z * 0.00072, 246810) + 1.0) * 0.5;
        double riverNoise = Math.abs(valueNoise(x * 0.0042, z * 0.0042, 771122));

        if (height < SEA_LEVEL - 8) {
            if (temperature > 0.78) return height < SEA_LEVEL - 24 ? Biome.DEEP_LUKEWARM_OCEAN : Biome.WARM_OCEAN;
            if (temperature < 0.20) return height < SEA_LEVEL - 24 ? Biome.DEEP_FROZEN_OCEAN : Biome.FROZEN_OCEAN;
            if (temperature < 0.38) return height < SEA_LEVEL - 24 ? Biome.DEEP_COLD_OCEAN : Biome.COLD_OCEAN;
            return height < SEA_LEVEL - 24 ? Biome.DEEP_OCEAN : Biome.OCEAN;
        }

        if (height <= SEA_LEVEL + 2 && riverNoise < 0.035) {
            return temperature < 0.22 ? Biome.FROZEN_RIVER : Biome.RIVER;
        }
        if (height <= SEA_LEVEL + 5) {
            if (temperature < 0.22) return Biome.SNOWY_BEACH;
            if (region > 0.88) return Biome.STONY_SHORE;
            return Biome.BEACH;
        }

        // Cold and high-altitude regions.
        if (temperature < 0.16) {
            if (height > 145) return Biome.FROZEN_PEAKS;
            return region < 0.50 ? Biome.SNOWY_PLAINS : Biome.ICE_SPIKES;
        }
        if (temperature < 0.29) return height > 125 ? Biome.SNOWY_SLOPES : (moisture > 0.48 ? Biome.OLD_GROWTH_SPRUCE_TAIGA : Biome.SNOWY_TAIGA);
        if (height > 170) return region > 0.5 ? Biome.JAGGED_PEAKS : Biome.STONY_PEAKS;
        if (height > 132) return moisture > 0.5 ? Biome.MEADOW : Biome.WINDSWEPT_GRAVELLY_HILLS;
        if (height > 112 && temperature < 0.48) return Biome.GROVE;

        // Hot and dry regions.
        if (temperature > 0.78 && moisture < 0.30) {
            if (region < 0.28) return Biome.ERODED_BADLANDS;
            if (region < 0.58) return Biome.BADLANDS;
            if (region < 0.78) return Biome.WOODED_BADLANDS;
            return Biome.DESERT;
        }
        if (temperature > 0.68 && moisture < 0.48) {
            return region < 0.45 ? Biome.SAVANNA : Biome.SAVANNA_PLATEAU;
        }

        // Wet tropical and temperate regions.
        if (temperature > 0.68 && moisture > 0.72) return region < 0.5 ? Biome.JUNGLE : Biome.BAMBOO_JUNGLE;
        if (temperature > 0.62 && moisture > 0.52) return region < 0.5 ? Biome.SPARSE_JUNGLE : Biome.MANGROVE_SWAMP;
        if (moisture > 0.82 && temperature > 0.42) return Biome.SWAMP;
        if (moisture > 0.72 && temperature < 0.45) return region < 0.5 ? Biome.OLD_GROWTH_PINE_TAIGA : Biome.TAIGA;

        // Temperate biome families. Small region-noise bands produce variants
        // such as flower forests, cherry groves, pale gardens and old-growth woods.
        if (region < 0.055) return Biome.MUSHROOM_FIELDS;
        if (region < 0.105) return Biome.CHERRY_GROVE;
        if (region < 0.155) return Biome.PALE_GARDEN;
        if (region < 0.205) return Biome.FLOWER_FOREST;
        if (region < 0.255) return Biome.SUNFLOWER_PLAINS;
        if (region < 0.305) return Biome.OLD_GROWTH_BIRCH_FOREST;
        if (region < 0.355) return Biome.BIRCH_FOREST;
        if (region < 0.405) return Biome.DARK_FOREST;
        if (region < 0.455) return Biome.WINDSWEPT_FOREST;
        if (region < 0.505) return Biome.WINDSWEPT_HILLS;
        if (region < 0.555) return Biome.FOREST;
        if (region < 0.605) return Biome.PLAINS;
        if (region < 0.655) return Biome.TAIGA;
        if (region < 0.705) return Biome.MEADOW;
        return moisture > 0.48 ? Biome.FOREST : Biome.PLAINS;
    }

    private static Material surfaceFor(Biome biome, int height) {
        if (biome == Biome.BEACH || biome == Biome.SNOWY_BEACH || biome == Biome.DESERT) return Material.SAND;
        if (biome == Biome.BADLANDS || biome == Biome.ERODED_BADLANDS || biome == Biome.WOODED_BADLANDS) return Material.RED_SAND;
        if (biome == Biome.STONY_SHORE || biome == Biome.STONY_PEAKS || biome == Biome.JAGGED_PEAKS || biome == Biome.WINDSWEPT_GRAVELLY_HILLS) return Material.STONE;
        if (biome == Biome.FROZEN_PEAKS || biome == Biome.SNOWY_SLOPES || biome == Biome.ICE_SPIKES || biome == Biome.SNOWY_PLAINS || biome == Biome.SNOWY_TAIGA || biome == Biome.GROVE) return Material.SNOW_BLOCK;
        if (biome == Biome.SWAMP || biome == Biome.MANGROVE_SWAMP) return Material.MUD;
        if (biome == Biome.MUSHROOM_FIELDS) return Material.MYCELIUM;
        return Material.GRASS_BLOCK;
    }

    private static Material fillerFor(Biome biome) {
        if (biome == Biome.BEACH || biome == Biome.SNOWY_BEACH || biome == Biome.DESERT) return Material.SANDSTONE;
        if (biome == Biome.BADLANDS || biome == Biome.ERODED_BADLANDS || biome == Biome.WOODED_BADLANDS) return Material.TERRACOTTA;
        if (biome == Biome.STONY_SHORE || biome == Biome.STONY_PEAKS || biome == Biome.JAGGED_PEAKS || biome == Biome.WINDSWEPT_GRAVELLY_HILLS) return Material.STONE;
        if (biome == Biome.SWAMP || biome == Biome.MANGROVE_SWAMP) return Material.MUD;
        return Material.DIRT;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double valueNoise(double x, double z, int seed) {
        int x0 = fastFloor(x), z0 = fastFloor(z), x1 = x0 + 1, z1 = z0 + 1;
        double tx = fade(x - x0), tz = fade(z - z0);
        double a = lerp(hashNoise(x0, z0, seed), hashNoise(x1, z0, seed), tx);
        double b = lerp(hashNoise(x0, z1, seed), hashNoise(x1, z1, seed), tx);
        return lerp(a, b, tz);
    }

    private static double hashNoise(int x, int z, int seed) {
        long n = (long) x * 341873128712L + (long) z * 132897987541L + seed * 42317861L;
        n = (n ^ (n >>> 13)) * 1274126177L;
        n ^= n >>> 16;
        return ((n & 0x7fffffffL) / 1073741823.5) - 1.0;
    }

    private static double hash01(int x, int y, int z) {
        long n = (long) x * 73428767L ^ (long) y * 912931L ^ (long) z * 42317861L;
        n = (n ^ (n >>> 16)) * 0x45d9f3bL;
        n = (n ^ (n >>> 16)) * 0x45d9f3bL;
        n ^= n >>> 16;
        return (n & 0x7fffffffL) / (double) Integer.MAX_VALUE;
    }

    private static int fastFloor(double value) { int i = (int) value; return value < i ? i - 1 : i; }
    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static final class TerraBiomeProvider extends BiomeProvider {
        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            // Cave biomes are selected underground; surface biome distribution
            // remains driven by temperature, moisture, elevation and region noise.
            if (y < -8) {
                double cave = (valueNoise(x * 0.012, z * 0.012, 515151) + 1.0) * 0.5;
                if (cave < 0.30) return Biome.LUSH_CAVES;
                if (cave < 0.66) return Biome.DRIPSTONE_CAVES;
                if (y < -35 && cave > 0.91) return Biome.DEEP_DARK;
            }
            return surfaceBiomeAt(x, z, terrainHeight(x, z));
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return OVERWORLD_BIOMES;
        }
    }
}
