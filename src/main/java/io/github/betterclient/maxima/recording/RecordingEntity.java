package io.github.betterclient.maxima.recording;

import com.mojang.authlib.GameProfile;
import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.util.FakeClientConnection;
import io.github.betterclient.maxima.util.FakeNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.StatHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RecordingEntity {
    public final byte[] data;
    public int entityID = 0;
    public String uuid = "helloworld";
    public boolean isPlayer = false;

    public RecordingEntity(Entity entity) {
        if (entity instanceof PlayerEntity)
            isPlayer = true;

        NbtCompound comp = new NbtCompound();
        entity.getCommandTags().add("maxima");
        if (!entity.saveNbt(comp))
            entity.writeNbt(comp);
        entity.getCommandTags().remove("maxima");

        if (isPlayer) {
            comp.putString("NAME_PLAYER", entity.getName().getString());
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

    public RecordingEntity(byte[] data) {
        this.data = data;
    }

    public static List<RecordingEntity> getCurrentList(ClientWorld world) {
        return new ArrayList<>(world.entityList.entities.values().stream().toList().stream().map(RecordingEntity::new).toList());
    }

    public void apply(Entity entity) throws IOException {
        if (data.length == 0) return;
        if (data.length == 1) return;

        NbtCompound comp = NbtCompound.TYPE.read(new DataInputStream(new ByteArrayInputStream(data)), NbtSizeTracker.ofUnlimitedBytes());
        entity.readNbt(comp);
    }

    public static int PX, PY, PZ;

    public Entity generate(ServerWorld world) throws IOException {
        if (data.length == 0) return null;
        if (data.length == 1) return null;

        NbtCompound comp = NbtCompound.TYPE.read(new DataInputStream(new ByteArrayInputStream(data)), NbtSizeTracker.ofUnlimitedBytes());
        if (isPlayer) {//EntityType.PLAYER.create is a bit stubborn.
            MinecraftClient client = MinecraftClient.getInstance();
            ServerPlayerEntity e = new ServerPlayerEntity(client.getServer(), world, new GameProfile(comp.getUuid("UUID"), comp.getString("NAME_PLAYER")), world.getRandomAlivePlayer().getClientOptions());
            e.readNbt(comp);
            e.networkHandler = new FakeNetworkHandler(e);
            PX = (int) e.getX();
            PY = (int) e.getY();
            PZ = (int) e.getZ();
            return e;
        }

        EntityType<? extends Entity> entityType = EntityType.get(comp.getString("id")).orElseThrow(NullPointerException::new);
        Entity e = entityType.create(world);
        if (e == null) return null;

        e.readNbt(comp);
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
        e.readNbt(comp);
        PX = (int) e.getX();
        PY = (int) e.getY();
        PZ = (int) e.getZ();
        return e;
    }
}
