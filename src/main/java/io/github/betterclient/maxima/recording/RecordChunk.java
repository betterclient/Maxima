package io.github.betterclient.maxima.recording;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class RecordChunk {
    public Map<BlockPos, BlockRecord> record = new HashMap<>();
    public int chunkX, chunkZ, bottomY, topY;

    public RecordChunk() {}

    public RecordChunk(WorldChunk wc, RecordChunk lc) {
        chunkX = wc.getPos().x;
        chunkZ = wc.getPos().z;
        bottomY = wc.getBottomY();
        topY = wc.getTopY();
        for (int x = 0; x < 16; x++) {
            for (int y = bottomY; y < topY; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    BlockState state = wc.getBlockState(pos);
                    if(lc != null) {
                        BlockRecord old = lc.blockAt(pos);
                        if (old != null) {
                            if (old.block().getBlock() == state.getBlock()) {
                                continue;
                            } else if (old.block().getBlock() != state.getBlock()) {
                                record.put(pos, new BlockRecord(pos, state));
                                continue;
                            }
                        }
                    }

                    if(state.getBlock().equals(Blocks.AIR) || state.getBlock().equals(Blocks.CAVE_AIR)) continue;

                    record.put(pos, new BlockRecord(pos, state));
                }
            }
        }
    }

    private BlockRecord blockAt(BlockPos pos) {
        return record.get(pos);
    }

    public RecordChunk upgrade(RecordChunk with) {
        if(this == with)
            return this;

        RecordChunk newI = new RecordChunk();
        newI.record = new HashMap<>(this.record);
        newI.record.putAll(with.record);
        return newI;
    }

    public record BlockRecord(BlockPos pos, BlockState block) {
        public String toString() {
            StringBuilder pos = new StringBuilder(pos().getX() + "," + pos().getY() + "," + pos().getZ());
            for (Property<?> property : block.getProperties()) {
                pos.append(",{").append(property.getName()).append(":").append(block.get(property)).append("}");
            }
            return "[" + pos + "]" + (block.getBlock().toString().replace("Block{", "").replace("}", ""));
        }
    }

    public static BlockRecord parse(String fromString) {
        Block block;
        int x = 0, y = 0, z = 0;
        Map<String, String> properties = new HashMap<>();

        String valueList = fromString.substring(1, fromString.indexOf(']'));
        String blockID = fromString.substring(fromString.indexOf(']') + 1);

        String[] items = valueList.split(",");
        List<String> values = Arrays.asList(items);

        for (int i = 0; i < values.size(); i++) {
            String val = values.get(i);
            if(i == 0) {
                x = Integer.parseInt(val);
            } else if(i == 1) {
                y = Integer.parseInt(val);
            } else if(i == 2) {
                z = Integer.parseInt(val);
            } else {
                String key = val.substring(1, val.indexOf(':'));
                String value = val.substring(val.indexOf(':') + 1);

                properties.put(key, value);
            }
        }

        block = Registries.BLOCK.get(Identifier.of(blockID));
        BlockState state = block.getDefaultState();

        //im sorry
        for (String s : properties.keySet()) {
            for (Property property : state.getProperties()) {
                if(property.getName().equals(s)) {
                    Optional<?> obj = property.parse(properties.get(s));
                    if(obj.isEmpty()) continue;

                    state = state.with(property, (Comparable) obj.get());
                }
            }
        }

        return new BlockRecord(new BlockPos(x, y, z), state);
    }
}
