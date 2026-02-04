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

    public boolean canSee(ServerPlayer viewer, ServerPlayer vanishedPlayer){
        return VISIBILITY_OVERRIDES
                .getOrDefault(vanishedPlayer.getUUID(), Set.of())
                .contains(viewer.getUUID());
    }

    public void allow(ServerPlayer viewer, ServerPlayer vanishedPlayer){
        VISIBILITY_OVERRIDES
                .computeIfAbsent(vanishedPlayer.getUUID(), k -> new HashSet<>())
                .add(viewer.getUUID());
        setDirty();
    }

    public void deny(ServerPlayer viewer, ServerPlayer vanishedPlayer){
        Set<UUID> set = VISIBILITY_OVERRIDES.get(vanishedPlayer.getUUID());
        if(set != null) {
            set.remove(viewer.getUUID());
            if(set.isEmpty()){
                VISIBILITY_OVERRIDES.remove(vanishedPlayer.getUUID());
            }
        }
        setDirty();
    }

    public void clear(ServerPlayer vanishedPlayer){
        VISIBILITY_OVERRIDES.remove(vanishedPlayer.getUUID());
        setDirty();
    }

    public List<ServerPlayer> getPlayersWhoCanSee(ServerPlayer vanishedPlayer){
        Set<UUID> viewers = getViewers(vanishedPlayer.getUUID());

        List<ServerPlayer> result = new ArrayList<>();

        for(UUID viewerUUID : viewers){
            ServerPlayer viewer = vanishedPlayer.server.getPlayerList().getPlayer(viewerUUID);
            if(viewer != null){
                result.add(viewer);
            }
        }

        return result;
    }

    private Set<UUID> getViewers(UUID vanishedPlayer){
        return VISIBILITY_OVERRIDES.getOrDefault(vanishedPlayer, Set.of());
    }
}
