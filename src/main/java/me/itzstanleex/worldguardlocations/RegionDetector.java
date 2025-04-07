package me.itzstanleex.worldguardlocations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RegionDetector {

    private final WorldguardLocations plugin;
    private final RegionContainer regionContainer;

    public RegionDetector(WorldguardLocations plugin) {
        this.plugin = plugin;
        this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    public String getHighestPriorityRegionAt(Location location) {
        if (location == null) {
            return null;
        }

        try {
            com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(location);
            com.sk89q.worldguard.protection.managers.RegionManager regions = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));

            if (regions == null) {
                return null;
            }

            ApplicableRegionSet applicableRegions = regions.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            List<ProtectedRegion> regionList = new ArrayList<>(applicableRegions.getRegions());

            regionList.sort(Comparator.comparingInt(ProtectedRegion::getPriority).reversed());

            if (!regionList.isEmpty()) {
                return regionList.get(0).getId();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Chyba při zjišťování regionu: " + e.getMessage());
        }

        return null;
    }
}