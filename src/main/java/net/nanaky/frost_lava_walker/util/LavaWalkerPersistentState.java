package net.nanaky.frost_lava_walker.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LavaWalkerPersistentState extends SavedData {

    public final Map<BlockPos, Long> startTimes = new HashMap<>();

    // Codec for a single entry: {pos: long, start: long}
    private static final Codec<Map.Entry<BlockPos, Long>> ENTRY_CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Map.Entry::getKey),
            Codec.LONG.fieldOf("start").forGetter(Map.Entry::getValue)
        ).apply(instance, Map::entry));

    // Codec for the full state: a list of entries
    private static final Codec<LavaWalkerPersistentState> CODEC =
        ENTRY_CODEC.listOf().xmap(
            entries -> {
                LavaWalkerPersistentState state = new LavaWalkerPersistentState();
                for (Map.Entry<BlockPos, Long> e : entries) {
                    state.startTimes.put(e.getKey(), e.getValue());
                }
                return state;
            },
            state -> List.copyOf(state.startTimes.entrySet())
        );

    public static final SavedDataType<LavaWalkerPersistentState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("frost_lava_walker", "pending_blocks"),
        LavaWalkerPersistentState::new,
        CODEC,
        null
    );

    public static LavaWalkerPersistentState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}