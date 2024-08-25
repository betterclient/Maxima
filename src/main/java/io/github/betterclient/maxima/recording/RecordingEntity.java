package io.github.betterclient.maxima.recording;

import com.mojang.authlib.GameProfile;
import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.util.FakeNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecordingEntity {
    public final byte[] data;
    public int entityID = 0;
    public String uuid = "helloworld";
    public boolean isPlayer = false;
    private NbtCompound generated = null;

    public RecordingEntity(Entity entity) {
        NbtCompound comp = new NbtCompound();
        entity.getCommandTags().add("maxima");
        if (!entity.saveNbt(comp))
            entity.writeNbt(comp);
        entity.getCommandTags().remove("maxima");

        if (entity instanceof PlayerEntity pe) {
            isPlayer = true;
            comp.putString("NAME_PLAYER", entity.getName().getString());
            comp.putFloat("HAND_SWING_PROGRESS", pe.handSwingProgress);
            comp.putInt("HAND_SWING_TICKS", pe.handSwingTicks);
            comp.putFloat("LIMB_ANIMATOR_POS", pe.limbAnimator.pos);
        }

        entityID = entity.getId();
        uuid = entity.getUuidAsString();

        try {
            ByteArrayOutputStream want = new ByteArrayOutputStream();
            comp.write(new DataOutputStream(want));
            data = want.toByteArray();
            want.close();
        } catch (IOException e) {
            MaximaClient.LOGGER.error("Something went terribly wrong", e);
            throw new RuntimeException("This should technically never happen. Did you break the jvm by chance?", e);
        }
    }

    public RecordingEntity(NbtCompound from) {
        data = new byte[0];
        generated = from;
    }

    public RecordingEntity(byte[] data) {
        this.data = data;
    }

    public static List<RecordingEntity> getCurrentList(ClientWorld world) {
        return new ArrayList<>(world.entityList.entities.values().stream().toList().stream().map(RecordingEntity::new).toList());
    }

    public NbtCompound generate() throws IOException {
        if (generated == null) {
            generated = NbtCompound.TYPE.read(new DataInputStream(new ByteArrayInputStream(data)), NbtSizeTracker.ofUnlimitedBytes());
        }

        return generated;
    }

    public void apply(Entity entity) throws IOException {
        NbtCompound comp = generate();
        entity.readNbt(comp);

        if (entity instanceof PlayerEntity pe) {
            pe.handSwingProgress = comp.getFloat("HAND_SWING_PROGRESS");
            pe.handSwingTicks = comp.getInt("HAND_SWING_TICKS");
            pe.limbAnimator.pos = comp.getFloat("LIMB_ANIMATOR_POS");

            pe.preferredHand = Hand.MAIN_HAND;
        }

        entity.setUuid(UUID.fromString(this.uuid));
    }

    public static int PX, PY, PZ;

    public Entity generate(ServerWorld world) throws IOException {
        if (data.length == 0) return null;
        if (data.length == 1) return null;

        NbtCompound comp = generate();
        if (isPlayer) {//EntityType.PLAYER.create is a bit stubborn.
            MinecraftClient client = MinecraftClient.getInstance();
            ServerPlayerEntity e = new ServerPlayerEntity(client.getServer(), world, new GameProfile(comp.getUuid("UUID"), comp.getString("NAME_PLAYER")), world.getRandomAlivePlayer().getClientOptions());
            this.apply(e);
            e.networkHandler = new FakeNetworkHandler(e);
            PX = (int) e.getX();
            PY = (int) e.getY();
            PZ = (int) e.getZ();
            return e;
        }

        EntityType<? extends Entity> entityType = EntityType.get(comp.getString("id")).orElseThrow(NullPointerException::new);
        Entity e = entityType.create(world);
        if (e == null) return null;

        this.apply(e);
        return e;
    }

    public String getPText() {
        return isPlayer ? "P" : "";
    }

    public Entity generate(ClientWorld world) throws IOException {
        if (data.length == 0) return null;
        if (data.length == 1) return null;

        NbtCompound comp = NbtCompound.TYPE.read(new DataInputStream(new ByteArrayInputStream(data)), NbtSizeTracker.ofUnlimitedBytes());
        if (!comp.contains("NAME_PLAYER")) return null;

        OtherClientPlayerEntity e = new OtherClientPlayerEntity(world, new GameProfile(comp.getUuid("UUID"), comp.getString("NAME_PLAYER")));
        this.apply(e);
        PX = (int) e.getX();
        PY = (int) e.getY();
        PZ = (int) e.getZ();
        return e;
    }

    public void updateUUID() throws IOException {
        generate().putUuid("UUID", UUID.fromString(this.uuid));
    }
}
