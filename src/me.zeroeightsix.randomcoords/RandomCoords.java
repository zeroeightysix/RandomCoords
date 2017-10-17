package me.zeroeightsix.randomcoords;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class RandomCoords extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        File tempFile = new File(getDescription().getName().toLowerCase() + ".tmp");
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            if (tempFile.exists())
                tempFile.delete();
        }else{
            if (tempFile.exists()) {
                try {
                    List<String> list = Files.readAllLines(tempFile.toPath(), Charset.defaultCharset() );
                    list.forEach(s -> {
                        String[] split = s.split(";");
                        if (split.length != 3) return;
                        String id = split[0];
                        Player p = Bukkit.getPlayer(UUID.fromString(id));
                        if (p == null || !p.isOnline()) return;
                        int x = Integer.parseInt(split[1]);
                        int z = Integer.parseInt(split[2]);
                        PlayerCoords.addNewPlayer(p, x, z);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tempFile.delete();
            }else{
                Bukkit.getOnlinePlayers().forEach(o -> PlayerCoords.addNewPlayer(o));
            }
        }

        Bukkit.getOnlinePlayers().forEach(o -> PlayerCoords.addNewPlayer(o));

        Bukkit.getPluginManager().registerEvents(this, this);
        final ProtocolManager pm = ProtocolLibrary.getProtocolManager();

        final HashSet<PacketType> packets = new HashSet<PacketType>();

        // /Server side packets
        {
            final PacketAdapter.AdapterParameteters paramsServer = PacketAdapter.params();
            paramsServer.plugin(this);
            paramsServer.connectionSide(ConnectionSide.SERVER_SIDE);
            paramsServer.listenerPriority(ListenerPriority.HIGHEST);
            paramsServer.gamePhase(GamePhase.BOTH);

            packets.add(PacketType.Play.Server.BED);
            packets.add(PacketType.Play.Server.BLOCK_ACTION);
            packets.add(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
            packets.add(PacketType.Play.Server.BLOCK_CHANGE);
            packets.add(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
            packets.add(PacketType.Play.Server.MAP_CHUNK);
            packets.add(PacketType.Play.Server.MAP_CHUNK_BULK);
            packets.add(PacketType.Play.Server.EXPLOSION);
            packets.add(PacketType.Play.Server.SPAWN_POSITION);

            packets.add(PacketType.Play.Server.RESPAWN);
            packets.add(PacketType.Play.Server.POSITION);

            packets.add(PacketType.Play.Server.WORLD_PARTICLES);
            packets.add(PacketType.Play.Server.WORLD_EVENT);

            packets.add(PacketType.Play.Server.NAMED_SOUND_EFFECT);

            packets.add(PacketType.Play.Server.SPAWN_ENTITY);
            packets.add(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            packets.add(PacketType.Play.Server.SPAWN_ENTITY_WEATHER);
            packets.add(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
            packets.add(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB);
            packets.add(PacketType.Play.Server.SPAWN_ENTITY_PAINTING);
            packets.add(PacketType.Play.Server.ENTITY_TELEPORT);

            packets.add(PacketType.Play.Server.TILE_ENTITY_DATA);
            packets.add(PacketType.Play.Server.OPEN_SIGN_EDITOR);

            paramsServer.types(packets);

            pm.addPacketListener(new PacketAdapter(paramsServer) {
                @Override
                public void onPacketSending(final PacketEvent event) {
                    if (event.getPacket().getType() == PacketType.Play.Server.TILE_ENTITY_DATA) {
                        System.out.println("HANDLING ENTITY DATA");
                    }

                    PacketContainer packet;

                    if (event.getPacket().getType() == PacketType.Play.Server.TILE_ENTITY_DATA) {
                        packet = RandomCoords.this.cloneTileEntityData(event.getPacket());
                        System.out.println("HANDLING ENTITY DATA");
                    }
                    else
                        packet = event.getPacket().shallowClone();

                    event.setPacket(packet);

                    Translate.outgoing(packet, event.getPlayer());
                }
            });
        }// End Server Packets

        // /Client side Packets
        {
            final PacketAdapter.AdapterParameteters paramsClient = PacketAdapter.params();
            paramsClient.plugin(this);
            paramsClient.connectionSide(ConnectionSide.CLIENT_SIDE);
            paramsClient.listenerPriority(ListenerPriority.LOWEST);
            paramsClient.gamePhase(GamePhase.BOTH);

            packets.clear();

            packets.add(PacketType.Play.Client.POSITION);
            packets.add(PacketType.Play.Client.POSITION_LOOK);
            packets.add(PacketType.Play.Client.BLOCK_PLACE);
            packets.add(PacketType.Play.Client.BLOCK_DIG);
            packets.add(PacketType.Play.Client.UPDATE_SIGN);
            packets.add(PacketType.Play.Client.USE_ITEM);
            packets.add(PacketType.Play.Client.VEHICLE_MOVE);

            paramsClient.types(packets);

            pm.addPacketListener(new PacketAdapter(paramsClient) {

            @Override
            public void onPacketReceiving(final PacketEvent event) {
                try {
                    Translate.incoming(event.getPacket(), event.getPlayer());
                } catch (final UnsupportedOperationException e) {
                    event.setCancelled(true);
                    Bukkit.getServer().broadcastMessage("Failed: " + event.getPacket().getType().name());
                }
            }
            });
        }

        System.out.println("RandomCoords enabled");
    }

    @Override
    public void onDisable() {
        if (Bukkit.getOnlinePlayers().size() > 0) {
            ArrayList<String> map = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers())
                map.add(player.getUniqueId().toString() + ";" + PlayerCoords.getX(player) + ";" + PlayerCoords.getZ(player));

            try {
                File tempFile = new File(getDescription().getName().toLowerCase() + ".tmp");
                if (!tempFile.exists())
                    tempFile.createNewFile();

                PrintWriter pw = new PrintWriter(new FileOutputStream(tempFile));
                for (String s : map)
                    pw.println(s); // call toString() on club, like club.toString()
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PlayerCoords.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        PlayerCoords.clean(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        PlayerCoords.addNewPlayer(event.getPlayer());
    }

    private PacketContainer cloneTileEntityData(final PacketContainer packet) {
        final PacketContainer newPacket = new PacketContainer(packet.getType());

        newPacket.getBlockPositionModifier().write(0, packet.getBlockPositionModifier().read(0).add(new BlockPosition(0,0,0)));
        newPacket.getIntegers().write(0, packet.getIntegers().read(0));
        newPacket.getNbtModifier().write(0, packet.getNbtModifier().read(0).deepClone());

//        int i = 0;
//        for (final Object obj : packet.getModifier().getValues()) {
//            newPacket.getModifier().write(i, obj);
//            i++;
//        }
//
//        i = 0;
//        for (final NbtBase<?> obj : packet.getNbtModifier().getValues()) {
//            newPacket.getNbtModifier().write(i, obj.deepClone());
//            i++;
//        }
//
        return newPacket;
    }
}
