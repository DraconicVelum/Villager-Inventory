package ca.henrychang.villagerinventory;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public final class VillagerInventory extends JavaPlugin {

    VillagerInvEventHandler eventHandler;
    HashMap<UUID, Villager> invMap;

    Material interactMaterial = Material.STICK;
    boolean dropOnDeath = true;

    @Override
    public void onEnable() {
        getLogger().info("Villager Inventory Plugin Version 2.5");
        getLogger().info("Villager Inventory Plugin Starting...");

        String key_material = "Interactive-Material";
        String key_dropOnDeath = "Drop-On-Death";

        String materialName = getConfig().getString(key_material);

        Material mat = null;
        if (materialName != null) {
            mat = Material.matchMaterial(materialName, true);
        }

        if (mat == null) {
            interactMaterial = Material.STICK;
            getConfig().set(key_material, interactMaterial.toString());
            saveConfig();
        } else {
            interactMaterial = mat;
        }

        if (!getConfig().contains(key_dropOnDeath)) {
            getConfig().set(key_dropOnDeath, dropOnDeath);
            saveConfig();
        } else {
            dropOnDeath = getConfig().getBoolean(key_dropOnDeath);
        }

        eventHandler = new VillagerInvEventHandler(this);
        getServer().getPluginManager().registerEvents(eventHandler, this);

        invMap = new HashMap<>();
    }

    @Override
    public void onDisable() {
        getLogger().info("Villager Inventory Plugin Stopping...");
        invMap.clear();
    }
}
