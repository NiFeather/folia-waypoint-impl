package xyz.nifeather.foliaWaypointImpl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class EventListener implements Listener
{
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWorldLoad(WorldInitEvent e)
    {
        WaypointManipulator.manipulate(e.getWorld());
    }
}
