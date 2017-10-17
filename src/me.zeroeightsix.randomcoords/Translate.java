package me.zeroeightsix.randomcoords;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.entity.Player;

import java.util.List;

public class Translate {

    public static void incoming(final PacketContainer packet, final Player player) {
        switch (packet.getType().name()) {
            case "VEHICLE_MOVE":
            case "POSITION":
            case "POSITION_LOOK":
                dOffsetAdd(packet, player, 0);
                break;
            case "BLOCK_DIG":
                pOffsetAdd(packet, player, 0);
                break;
            case "UPDATE_SIGN":
                pOffsetAdd(packet, player, 0);
                break;
            case "BLOCK_PLACE":
                if (!isUsageBlockPlace(packet))
                    iOffsetAdd(packet, player, 0);
                break;
            case "USE_ITEM":
                pOffsetAdd(packet, player, 0);
                break;
        }
    }

    public static void outgoing(final PacketContainer packet, final Player player) {
        switch (packet.getType().name()) {
            case "SPAWN_POSITION":
            case "SPAWN_ENTITY_PAINTING":
            case "BLOCK_CHANGE":
            case "WORLD_EVENT":
            case "BED":
            case "BLOCK_ACTION":
                pOffsetSubtract(packet, player, 0);
                break;
            case "POSITION":
            case "NAMED_ENTITY_SPAWN":
            case "SPAWN_ENTITY":
            case "SPAWN_ENTITY_LIVING":
            case "SPAWN_ENTITY_EXPERIENCE_ORB":
            case "ENTITY_TELEPORT":
            case "SPAWN_ENTITY_WEATHER":
                dOffsetSubtract(packet, player, 0);
                break;
            case "UPDATE_SIGN":
            case "OPEN_SIGN_ENTITY":
                lOffsetSubtract(packet, player, 0);
                break;
            case "TILE_ENTITY_DATA":
                iOffsetTileEntityData(packet, player);
                break;
            case "BLOCK_BREAK_ANIMATION":
                lOffsetSubtract(packet, player, 1);
                break;
            case "RESPAWN":
                PlayerCoords.addNewPlayer(player);
                break;
            case "MAP_CHUNK":
                iOffsetChunk(packet, player);
                break;
            case "MULTI_BLOCK_CHANGE":
                sendChunkUpdate(packet, player);
                break;
            case "MAP_CHUNK_BULK":
                sendChunkBulk(packet, player);
                break;
            case "EXPLOSION":
                iOffsetExplosion(packet, player);
                break;
            case "NAMED_SOUND_EFFECT":
                iOffsetSoundEffect(packet, player, 0);
                break;
            case "WORLD_PARTICLES":
                fOffsetWParticles(packet, player, 0);
                break;
        }
    }

    private static void dOffsetSubtract(final PacketContainer packet, final Player player, final int index) {
        final double curr_x = packet.getDoubles().read(index + 0).doubleValue();
        final double curr_z = packet.getDoubles().read(index + 2).doubleValue();

        packet.getDoubles().write(index + 0, curr_x - PlayerCoords.getX(player));
        packet.getDoubles().write(index + 2, curr_z - PlayerCoords.getZ(player));
    }

    private static void dOffsetAdd(final PacketContainer packet, final Player player, final int index) {
        final double curr_x = packet.getDoubles().read(index + 0).doubleValue();
        final double curr_z = packet.getDoubles().read(index + 2).doubleValue();

        packet.getDoubles().write(index + 0, curr_x + PlayerCoords.getX(player));
        packet.getDoubles().write(index + 2, curr_z + PlayerCoords.getZ(player));
    }

    private static boolean isUsageBlockPlace(final PacketContainer packet) {
        if (packet.getBlockPositionModifier().size() <= 0) return true;
        return false;
    }

    private static void iOffsetAdd(final PacketContainer packet, final Player player, final int index) {
        final int curr_x = packet.getIntegers().read(index + 0);
        final int curr_z = packet.getIntegers().read(index + 2);

        packet.getIntegers().write(index + 0, curr_x + PlayerCoords.getX(player));
        packet.getIntegers().write(index + 2, curr_z + PlayerCoords.getZ(player));
    }

    private static void iOffsetChunk(final PacketContainer packet, final Player player) {
        final int curr_x = packet.getIntegers().read(0);
        final int curr_z = packet.getIntegers().read(1);

        packet.getIntegers().write(0, curr_x - PlayerCoords.getChunkX(player));
        packet.getIntegers().write(1, curr_z - PlayerCoords.getChunkZ(player));
    }

    private static void sendChunkBulk(final PacketContainer packet, final Player player) {
        final int[] x = packet.getIntegerArrays().read(0).clone();
        final int[] z = packet.getIntegerArrays().read(1).clone();

        for (int i = 0; i < x.length; i++) {

            x[i] = x[i] - PlayerCoords.getChunkX(player);
            z[i] = z[i] - PlayerCoords.getChunkZ(player);
        }

        packet.getIntegerArrays().write(0, x);
        packet.getIntegerArrays().write(1, z);
    }

    private static void sendChunkUpdate(final PacketContainer packet, final Player player) {

        final ChunkCoordIntPair newCoords = new ChunkCoordIntPair(packet.getChunkCoordIntPairs().read(0).getChunkX()
                - PlayerCoords.getChunkX(player), packet.getChunkCoordIntPairs().read(0).getChunkZ() - PlayerCoords.getChunkZ(player));

        packet.getChunkCoordIntPairs().write(0, newCoords);
    }

    private static void iOffsetExplosion(final PacketContainer packet, final Player player) {
        dOffsetSubtract(packet, player, 0);

        List<BlockPosition> positions = packet.getBlockPositionCollectionModifier().read(0);
        BlockPosition offset = new BlockPosition(PlayerCoords.getX(player), 0, PlayerCoords.getZ(player));
        for (int i = 0; i < positions.size(); i++) {
            BlockPosition position = positions.get(i);
            positions.set(i, position.subtract(offset));
        }
        packet.getBlockPositionCollectionModifier().write(0, positions);
    }

    private static void fOffsetWParticles(final PacketContainer packet, final Player player, final int index) {
        final float curr_x = packet.getFloat().read(index + 0).floatValue();
        final float curr_z = packet.getFloat().read(index + 2).floatValue();

        packet.getFloat().write(index + 0, curr_x - PlayerCoords.getX(player));
        packet.getFloat().write(index + 2, curr_z - PlayerCoords.getZ(player));
    }

    private static void pOffsetSubtract(final PacketContainer packetContainer, final Player player, final int index) {
        BlockPosition position = packetContainer.getBlockPositionModifier().read(index);
        position = position.add(new BlockPosition(-PlayerCoords.getX(player), 0 , -PlayerCoords.getZ(player)));
        packetContainer.getBlockPositionModifier().write(index, position);
    }

    private static void pOffsetAdd(final PacketContainer packetContainer, final Player player, final int index) {
        BlockPosition position = packetContainer.getBlockPositionModifier().read(index);
        position = position.add(new BlockPosition(PlayerCoords.getX(player), 0 , PlayerCoords.getZ(player)));
        packetContainer.getBlockPositionModifier().write(index, position);
    }

    private static void lOffsetSubtract(final PacketContainer packet, final Player player, final int index) {
        long position = packet.getLongs().read(index); // Read the position

        // Decode
        int x = (int) position >> 38;
        int y = (int) (position >> 26) & 0xFFF;
        int z = (int) position << 38 >> 38;

        // Offset
        x -= PlayerCoords.getX(player);
        z -= PlayerCoords.getZ(player);

        // Encode
        position = ((x & 0x3FFFFFF) << 38) | ((y & 0xFFF) << 26) | (z & 0x3FFFFFF);

        // Write
        packet.getLongs().write(index, position);
    }

    private static void iOffsetSoundEffect(final PacketContainer packet, final Player player, final int index) {
        final int curr_x = packet.getIntegers().read(index + 0);
        final int curr_z = packet.getIntegers().read(index + 2);

        packet.getIntegers().write(index + 0, curr_x - (PlayerCoords.getX(player) << 3));
        packet.getIntegers().write(index + 2, curr_z - (PlayerCoords.getZ(player) << 3));
    }

    private static void iOffsetTileEntityData(final PacketContainer packet, final Player player) {
        BlockPosition position = packet.getBlockPositionModifier().read(0);
        pOffsetSubtract(packet, player, 0);

        NbtCompound nbt = (NbtCompound) packet.getNbtModifier().read(0);
        nbt.put("x", position.getX() - PlayerCoords.getX(player));
        nbt.put("z", position.getZ() - PlayerCoords.getZ(player));


    }
}
