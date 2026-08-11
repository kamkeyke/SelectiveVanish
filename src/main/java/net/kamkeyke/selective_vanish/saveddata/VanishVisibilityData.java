package net.kamkeyke.selective_vanish.saveddata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VanishVisibilityData extends SavedData {
    private static final String NAME = "visibility_overrides";
    private final Map<UUID, Set<UUID>> VISIBILITY_OVERRIDES = new HashMap<>();

    public static VanishVisibilityData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(
                VanishVisibilityData::load,
                VanishVisibilityData::new,
                NAME
        );
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        CompoundTag mapTag = new CompoundTag();

        VISIBILITY_OVERRIDES.forEach((vanishedPlayer, viewers) -> {
            ListTag listTag = new ListTag();
            viewers.forEach(uuid -> listTag.add(StringTag.valueOf(uuid.toString())));
            mapTag.put(vanishedPlayer.toString(), listTag);
        });

        tag.put("visibility", mapTag);
        return tag;
    }

    public static VanishVisibilityData load(CompoundTag tag){
        VanishVisibilityData data = new VanishVisibilityData();

        CompoundTag mapTag = tag.getCompound("visibility");

        for(String key : mapTag.getAllKeys()){
            UUID vanishedPlayer = UUID.fromString(key);
            ListTag listTag = mapTag.getList(key, Tag.TAG_STRING);

            Set<UUID> viewers = new HashSet<>();
            for(int i = 0; i < listTag.size(); i++){
                viewers.add(UUID.fromString(listTag.getString(i)));
            }

            data.VISIBILITY_OVERRIDES.put(vanishedPlayer, viewers);
        }

        return data;
    }

    // ------------------------ API --------------------------

    public boolean canSee(UUID viewerUUID, UUID vanishedPlayerUUID){
        return VISIBILITY_OVERRIDES
                .getOrDefault(vanishedPlayerUUID, Set.of())
                .contains(viewerUUID);
    }

    public void allow(UUID viewerUUID, UUID vanishedPlayerUUID){
        VISIBILITY_OVERRIDES
                .computeIfAbsent(vanishedPlayerUUID, k -> new HashSet<>())
                .add(viewerUUID);
        setDirty();
    }

    public void deny(UUID viewerUUID, UUID vanishedPlayerUUID){
        Set<UUID> set = VISIBILITY_OVERRIDES.get(vanishedPlayerUUID);
        if(set != null) {
            set.remove(viewerUUID);
            if(set.isEmpty()){
                VISIBILITY_OVERRIDES.remove(vanishedPlayerUUID);
            }
        }
        setDirty();
    }

    public void clear(UUID vanishedPlayerUUID){
        VISIBILITY_OVERRIDES.remove(vanishedPlayerUUID);
        setDirty();
    }

    public Set<UUID> getPlayersWhoCanSee(UUID vanishedPlayer){
        return VISIBILITY_OVERRIDES.getOrDefault(vanishedPlayer, Set.of());
    }
}
