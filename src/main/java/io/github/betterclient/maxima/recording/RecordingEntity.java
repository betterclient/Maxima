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
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.io.*;
import java.util.*;

public class RecordingEntity {
    public final byte[] data;
    public int entityID = 0;
    public String uuid = "helloworld";
    public boolean isPlayer = false;

    public Map<String, RecordingPart> compMap = Map.of(
            "LEFT_LEG", new RecordingPart(bpe -> bpe.leftLeg),
            "RIGHT_LEG", new RecordingPart(bpe -> bpe.rightLeg),
            "HEAD", new RecordingPart(bpe -> bpe.head),
            "HAT", new RecordingPart(bpe -> bpe.hat),
            "BODY", new RecordingPart(bpe -> bpe.body),
            "RIGHT_ARM", new RecordingPart(bpe -> bpe.rightArm),
            "LEFT_ARM", new RecordingPart(bpe -> bpe.leftArm)
    );

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

        comp.putFloat("HEAD_YAW", entity.getHeadYaw());
        comp.putFloat("BODY_YAW", entity.getBodyYaw());

        entityID = entity.getId();
        uuid = entity.getUuidAsString();

        try {
            data = toByteArray(comp);
        } catch (IOException e) {
            MaximaClient.LOGGER.error("Something went terribly wrong", e);
            throw new RuntimeException("This should technically never happen. Did you break the jvm by chance?", e);
        }
    }

    public byte[] toByteArray(NbtCompound comp) throws IOException {
        ByteArrayOutputStream want = new ByteArrayOutputStream();
        comp.write(new DataOutputStream(want));
        byte[] data = want.toByteArray();
        want.close();
        return data;
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

        for (String key : generated.getKeys()) {
            if (compMap.containsKey(key)) {
                compMap.get(key).comp.copyFrom(generated.getCompound(key));
            }
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

        NbtList nbt = comp.getList("Motion", NbtElement.DOUBLE_TYPE);
        entity.setVelocityClient(nbt.getDouble(0), nbt.getDouble(1), nbt.getDouble(2));

        entity.setHeadYaw(comp.getFloat("HEAD_YAW"));
        entity.setBodyYaw(comp.getFloat("BODY_YAW"));

        entity.setUuid(UUID.fromString(this.uuid));
    }

    public static int PX = 0, PY = 128, PZ = 0;

    public Entity generate(ServerWorld world) throws IOException {
        NbtCompound comp = generate();
        if (isPlayer) {//EntityType.PLAYER.create is a bit stubborn.
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getServer() == null) return null;

            ServerPlayerEntity e = new ServerPlayerEntity(client.getServer(), world, new GameProfile(comp.getUuid("UUID"), comp.getString("NAME_PLAYER")), Objects.requireNonNull(world.getRandomAlivePlayer()).getClientOptions());
            this.apply(e);
            e.networkHandler = new FakeNetworkHandler(e);
            PX = (int) e.getX();
            PY = (int) e.getY();
            PZ = (int) e.getZ();
            return e;
        }

        EntityType<? extends Entity> entityType = EntityType.get(comp.getString("id")).orElse(null);
        if (entityType == null) return null;
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

    public byte[] appendLegs() throws IOException {
        NbtCompound comp = generate();
        compMap.forEach((string, recordingPart) -> comp.put(string, recordingPart.comp));
        return toByteArray(comp);
    }
}
