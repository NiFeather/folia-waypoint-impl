package xyz.nifeather.foliaWaypointImpl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiConsumer;

public class SynchronizedWaypointManager extends ServerWaypointManager
{
    private static final Logger logger = LoggerFactory.getLogger("SynchronizedWaypointManager");

    public SynchronizedWaypointManager()
    {
        doReplace();

        //region createConnection
        BiConsumer<ServerPlayer, WaypointTransmitter> createConn = (a, b) -> {};
        try
        {
            var method = this.getClass().getSuperclass().getDeclaredMethod("createConnection", ServerPlayer.class, WaypointTransmitter.class);
            method.setAccessible(true);

            createConn = (player, transmitter) ->
            {
                try
                {
                    method.invoke(this, player, transmitter);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    logger.error("Unable to call method 'createConnection'", e);
                }
            };
        }
        catch (Throwable t)
        {
            logger.error("Failed getting createConnection method", t);
        }

        createConnectionConsumer = createConn;
        //endregion createConnection

        //region updateConnection
        TriConsumer<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> updateConn = (a, b, c) -> {};

        try
        {
            var method = this.getClass().getSuperclass().getDeclaredMethod("updateConnection", ServerPlayer.class, WaypointTransmitter.class, WaypointTransmitter.Connection.class);
            method.setAccessible(true);
            updateConn = (player, waypoint, connection) ->
            {
                try
                {
                    method.invoke(this, player, waypoint, connection);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    logger.error("Unable to call method 'updateConnection'", e);
                }
            };
        }
        catch (Throwable t)
        {
            logger.error("Failed getting updateConnection method", t);
        }

        updateConnectionConsumer = updateConn;
        //endregion updateConnection
    }

    private final Set<WaypointTransmitter> synchronizedWaypoints = Collections.synchronizedSet(new HashSet<>());
    private final Set<ServerPlayer> synchronizedPlayers = Collections.synchronizedSet(new HashSet<>());
    private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> synchronizedConnections = new SynchronizedForwardingTable<>(HashBasedTable.create());

    private final BiConsumer<ServerPlayer, WaypointTransmitter> createConnectionConsumer;
    private final TriConsumer<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> updateConnectionConsumer;

    private void doReplace()
    {
        logger.info("Starting to replace fields...");

        try
        {
            replaceWaypoint();
            replacePlayers();
            replaceTable();

            logger.info("Done replacing fields!");
        }
        catch (Throwable t)
        {
            logger.error("Failed to replace fields!", t);
        }
    }

    private void replaceWaypoint() throws NoSuchFieldException, IllegalAccessException
    {
        logger.info("Starting replace waypoint...");

        var field = this.getClass().getSuperclass().getDeclaredField("waypoints");
        field.setAccessible(true);
        field.set(this, synchronizedWaypoints);
    }

    private void replacePlayers() throws NoSuchFieldException, IllegalAccessException
    {
        logger.info("Starting replace players...");

        var field = this.getClass().getSuperclass().getDeclaredField("players");
        field.setAccessible(true);
        field.set(this, synchronizedPlayers);
    }

    private void replaceTable() throws NoSuchFieldException, IllegalAccessException
    {
        logger.info("Starting replace connections...");

        var field = this.getClass().getSuperclass().getDeclaredField("connections");
        field.setAccessible(true);
        field.set(this, synchronizedConnections);
    }

    private void createConnection(ServerPlayer player, WaypointTransmitter waypointTransmitter)
    {
        createConnectionConsumer.accept(player, waypointTransmitter);
    }

    private void updateConnection(ServerPlayer player, WaypointTransmitter waypoint, WaypointTransmitter.Connection connection)
    {
        updateConnectionConsumer.accept(player, waypoint, connection);
    }

    @Override
    public void trackWaypoint(WaypointTransmitter waypoint)
    {
        this.synchronizedWaypoints.add(waypoint);

        for (ServerPlayer serverPlayer : this.synchronizedPlayers)
            this.createConnection(serverPlayer, waypoint);
    }

    @Override
    public void addPlayer(ServerPlayer player)
    {
        this.synchronizedPlayers.add(player);

        for (WaypointTransmitter transmitter : this.synchronizedWaypoints)
            this.createConnection(player, transmitter);

        if (player.isTransmittingWaypoint())
            this.trackWaypoint(player);
    }

    @Override
    public void updatePlayer(ServerPlayer player)
    {
        var map = this.synchronizedConnections.row(player);
        var set = Sets.difference(this.synchronizedWaypoints, map.keySet());

        for (Map.Entry<WaypointTransmitter, WaypointTransmitter.Connection> entry : ImmutableSet.copyOf(map.entrySet()))
            this.updateConnection(player, entry.getKey(), entry.getValue());

        for (WaypointTransmitter transmitter : set)
            this.createConnection(player, transmitter);
    }

    @Override
    public void removePlayer(ServerPlayer player)
    {
        this.synchronizedConnections.row(player).values().removeIf(connection ->
        {
            connection.disconnect();
            return true;
        });

        this.untrackWaypoint(player);
        this.synchronizedPlayers.remove(player);
    }

    public static interface TriConsumer<A, B, C>
    {
        void accept(A first, B second, C third);
    }

    public static class SynchronizedForwardingTable<R, C, V> implements Table<R, C, V>
    {
        private final Object mutex = new Object();
        private final Table<R, C, V> table;

        public SynchronizedForwardingTable(Table<R, C, V> table)
        {
            this.table = table;
        }

        protected @NotNull Table<R, C, V> delegate()
        {
            return table;
        }

        @Override
        public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey)
        {
            synchronized (mutex)
            {
                return delegate().contains(rowKey, columnKey);
            }
        }

        @Override
        public boolean containsRow(@Nullable Object rowKey)
        {
            synchronized (mutex)
            {
                return delegate().containsRow(rowKey);
            }
        }

        @Override
        public boolean containsColumn(@Nullable Object columnKey)
        {
            synchronized (mutex)
            {
                return delegate().containsColumn(columnKey);
            }
        }

        @Override
        public boolean containsValue(@Nullable Object value)
        {
            synchronized (mutex)
            {
                return delegate().containsValue(value);
            }
        }

        @Override
        public @Nullable V get(@Nullable Object rowKey, @Nullable Object columnKey)
        {
            synchronized (mutex)
            {
                return delegate().get(rowKey, columnKey);
            }
        }

        @Override
        public boolean isEmpty()
        {
            synchronized (mutex)
            {
                return delegate().isEmpty();
            }
        }

        @Override
        public int size()
        {
            synchronized (mutex)
            {
                return delegate().size();
            }
        }

        @Override
        public void clear()
        {
            synchronized (mutex)
            {
                delegate().clear();
            }
        }

        @Override
        public @Nullable V put(R rowKey, C columnKey, V value)
        {
            synchronized (mutex)
            {
                return delegate().put(rowKey, columnKey, value);
            }
        }

        @Override
        public void putAll(@NotNull Table<? extends R, ? extends C, ? extends V> table)
        {
            synchronized (mutex)
            {
                delegate().putAll(table);
            }
        }

        @Override
        public @Nullable V remove(@Nullable Object rowKey, @Nullable Object columnKey)
        {
            synchronized (mutex)
            {
                return delegate().remove(rowKey, columnKey);
            }
        }

        @Override
        public @NotNull Map<C, V> row(R rowKey)
        {
            synchronized (mutex)
            {
                return delegate().row(rowKey);
            }
        }

        @Override
        public @NotNull Map<R, V> column(C columnKey)
        {
            synchronized (mutex)
            {
                return delegate().column(columnKey);
            }
        }

        @Override
        public @NotNull Set<Cell<R, C, V>> cellSet()
        {
            synchronized (mutex)
            {
                return delegate().cellSet();
            }
        }

        @Override
        public @NotNull Set<R> rowKeySet()
        {
            synchronized (mutex)
            {
                return delegate().rowKeySet();
            }
        }

        @Override
        public @NotNull Set<C> columnKeySet()
        {
            synchronized (mutex)
            {
                return delegate().columnKeySet();
            }
        }

        @Override
        public @NotNull Collection<V> values()
        {
            synchronized (mutex)
            {
                return delegate().values();
            }
        }

        @Override
        public @NotNull Map<R, Map<C, V>> rowMap()
        {
            synchronized (mutex)
            {
                return delegate().rowMap();
            }
        }

        @Override
        public @NotNull Map<C, Map<R, V>> columnMap()
        {
            synchronized (mutex)
            {
                return delegate().columnMap();
            }
        }
    }
}
