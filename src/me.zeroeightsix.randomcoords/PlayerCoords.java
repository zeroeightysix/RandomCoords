package me.zeroeightsix.randomcoords;

import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCoords {

    private static ConcurrentHashMap<UUID, int[]> playerCoords = new ConcurrentHashMap<UUID, int[]>();
    private final static Random rand = new Random();
    private static final int MINIMUM_OFFSET = -65000;

    public static void addNewPlayer(final Player player) {
        final int offsetX = MINIMUM_OFFSET + rand.nextInt(-2 * MINIMUM_OFFSET);
        final int offsetZ = MINIMUM_OFFSET + rand.nextInt(-2 * MINIMUM_OFFSET);
        addNewPlayer(player, offsetX, offsetZ);
    }

    public static void addNewPlayer(final Player player, int offsetX, int offsetZ) {
        playerCoords.put(player.getUniqueId(), new int[]{roundToClosest16(player.getLocation().getBlockX() + offsetX),
                roundToClosest16(player.getLocation().getBlockZ() + offsetZ)});
    }

    public static void clean(final Player player) {
        playerCoords.remove(player.getUniqueId());
    }

    public static void clear() {
        playerCoords.clear();
    }

    public static int getChunkX(final Player player) {
        return playerCoords.get(player.getUniqueId())[0] / 16;
    }

    public static int getChunkZ(final Player player) {
        return playerCoords.get(player.getUniqueId())[1] / 16;
    }

    public static int getX(final Player player) {
        return playerCoords.get(player.getUniqueId())[0];
    }

    public static int getZ(final Player player) {
        return playerCoords.get(player.getUniqueId())[1];
    }

    private static int roundToClosest16(final double d) {
        return (int) (Math.round(d / 16f) * 16);
    }

}
