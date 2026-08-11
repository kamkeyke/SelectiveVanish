package net.kamkeyke.selective_vanish.saveddata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class KnownPlayersData extends SavedData {
    private static final String NAME = "known_players";
    private final Map<UUID, String> uuidToName = new HashMap<>();
    private final Map<String, UUID> nameToUUID = new HashMap<>();

    public static KnownPlayersData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(
                KnownPlayersData::load,
                KnownPlayersData::new,
                NAME
        );
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag players = new ListTag();

        uuidToName.forEach((uuid, name) -> {
            CompoundTag player = new CompoundTag();
            player.putUUID("UUID", uuid);
            player.putString("Name", name);
            players.add(player);
        });

        tag.put("players", players);
        return tag;
    }

    public static KnownPlayersData load(CompoundTag tag){
        KnownPlayersData data = new KnownPlayersData();

        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);

        for (int i = 0; i < players.size(); i++){
            CompoundTag player = players.getCompound(i);

            UUID uuid = player.getUUID("UUID");
            String name = player.getString("Name");

            data.uuidToName.put(uuid, name);
            data.nameToUUID.put(name.toLowerCase(Locale.ROOT), uuid);
        }

        return data;
    }

    // ------------------------ API --------------------------

    public void cache(ServerPlayer player){
        UUID uuid = player.getUUID();
        String name = player.getGameProfile().getName();

        String previousName = uuidToName.put(uuid, name);

        if(Objects.equals(previousName, name)) return;

        if(previousName != null){
            nameToUUID.remove(previousName.toLowerCase(Locale.ROOT));
        }

        nameToUUID.put(name.toLowerCase(Locale.ROOT), uuid);

        setDirty();
    }

    public void remove(ServerPlayer player){
        UUID uuid = player.getUUID();
        String name = player.getGameProfile().getName();

        String previousName = uuidToName.remove(uuid);
        if(previousName == null) return;

        nameToUUID.remove(previousName.toLowerCase(Locale.ROOT));
        setDirty();
    }

    public String getName(UUID uuid){
        return uuidToName.get(uuid);
    }

    public UUID getUUID(String name) {
        return nameToUUID.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean contains(UUID uuid) {
        return uuidToName.containsKey(uuid);
    }

    public boolean contains(String name) {
        return nameToUUID.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Map<UUID, String> getAllUUIDToName(){
        return Collections.unmodifiableMap(uuidToName);
    }

    public Map<String, UUID> getAllNameToUUID() {
        return Collections.unmodifiableMap(nameToUUID);
    }
}