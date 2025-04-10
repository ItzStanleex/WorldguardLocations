package me.itzstanleex.worldguardlocations;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class WorldguardLocations extends JavaPlugin implements Listener {

    private RegionContainer regionContainer;
    private RegionQuery regionQuery;
    private YamlDocument config;
    private final Map<UUID, String> playerRegions = new ConcurrentHashMap<>();
    private  RegionDetector regionDetector;
    private final LocationPlaceholder placeholderExpansion = new LocationPlaceholder(this);

    @Override
    public void onEnable() {

        if (!checkDependencies()) {
            getLogger().severe("Plugin se vypíná kvůli chybějícím závislostím!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        regionQuery = regionContainer.createQuery();

        regionDetector = new RegionDetector(this);

        try {
            loadConfig();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Nepodařilo se načíst konfiguraci!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        if (placeholderExpansion.register()) {
            getLogger().info("PlaceholderAPI expanze úspěšně zaregistrována!");
        } else {
            getLogger().severe("Nepodařilo se zaregistrovat PlaceholderAPI expanzi!");
        }

        getLogger().info("Plugin úspěšně spuštěn!");
    }

    @Override
    public void onDisable() {
        placeholderExpansion.unregister();
        getLogger().info("Plugin byl vypnut!");
    }

    private boolean checkDependencies() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard nenalezen! Plugin vyžaduje WorldGuard.");
            return false;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI nenalezen! Plugin vyžaduje PlaceholderAPI.");
            return false;
        }
        return true;
    }

    private void loadConfig() throws IOException {
        config = YamlDocument.create(
                new File(getDataFolder(), "config.yml"),
                getResource("config.yml"),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build()
        );

        config.reload();
    }

    public YamlDocument getConfiguration() {
        return config;
    }

    public String getRegionDisplayName(String regionId) {
        if (regionId == null || regionId.isEmpty()) {
            return config.getString("default-location", "Neznámá oblast");
        }

        String path = "regions." + regionId + ".placeholder-name";
        return config.getString(path, regionId);
    }

    public String getPlayerRegion(Player player) {
        String region = playerRegions.get(player.getUniqueId());
        if (region == null || "global".equalsIgnoreCase(region)) {
            return config.getString("default-location", "Neznámá oblast");
        }
        return region;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Player player = event.getPlayer();
            String currentRegion = regionDetector.getHighestPriorityRegionAt(player.getLocation());
            String currentStoredRegion = playerRegions.get(player.getUniqueId());

            if (currentRegion == null || "global".equalsIgnoreCase(currentRegion)) {
                String defaultLocation = config.getString("default-location", "Neznámá oblast");
                playerRegions.put(player.getUniqueId(), defaultLocation);
            } else if (currentStoredRegion == null || !currentRegion.equals(currentStoredRegion)) {
                playerRegions.put(player.getUniqueId(), currentRegion);
            }
        });
    }

    private class LocationPlaceholder extends PlaceholderExpansion {

        private final WorldguardLocations plugin;

        public LocationPlaceholder(WorldguardLocations plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NotNull String getIdentifier() {
            return "dreamrealms";
        }

        @Override
        public @NotNull String getAuthor() {
            return "ItzStanleex";
        }

        @Override
        public @NotNull String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onPlaceholderRequest(Player player, @NotNull String identifier) {
            if (player == null) {
                return "";
            }

            if (identifier.equals("location")) {
                String regionId = plugin.getPlayerRegion(player);
                return plugin.getRegionDisplayName(regionId);
            }

            return null;
        }
    }
}