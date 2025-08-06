package xyz.nifeather.foliaWaypointImpl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaWaypointImpl extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

        Bukkit.getPluginManager().registerEvents(new EventListener(), this);
        getSLF4JLogger().info("Enabled FoliaWaypointImpl");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
