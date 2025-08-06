package xyz.nifeather.foliaWaypointImpl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.waypoints.ServerWaypointManager;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaypointManipulator
{
    private static ServerLevel nmsWorld(World world)
    {
        return ((CraftWorld)world).getHandle();
    }

    private static final Logger logger = LoggerFactory.getLogger("WaypointManipulator");

    public static void manipulate(World world)
    {
        var level = nmsWorld(world);

        logger.info("Going to manipulate world %s".formatted(world.getName()));

        try
        {
            var field = level.getClass().getDeclaredField("waypointManager");
            field.setAccessible(true);

            var original = (ServerWaypointManager) field.get(level);
            var newInstance = new SynchronizedWaypointManager();

            original.transmitters().forEach(newInstance::trackWaypoint);
            field.set(level, newInstance);

            logger.info("Done replacing waypoint manager of world %s!".formatted(world.getName()));
        }
        catch (Throwable t)
        {
            logger.error("Failed to replace waypoint manager", t);
        }
    }
}
