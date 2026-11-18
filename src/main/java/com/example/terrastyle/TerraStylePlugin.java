package com.example.terrastyle;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerraStylePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("TerraStyle enabled for the existing Overworld named 'world'.");
        getLogger().info("Only newly generated chunks will use TerraStyle terrain.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (!"world".equalsIgnoreCase(worldName)) {
            return null;
        }
        getLogger().info("Providing TerraStyle generator for world: " + worldName);
        return new TerraStyleGenerator();
    }
}
