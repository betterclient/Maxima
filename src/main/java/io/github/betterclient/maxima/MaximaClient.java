package io.github.betterclient.maxima;

import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.recording.RecordingEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Unique;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;

public class MaximaClient implements ClientModInitializer {
    public static int OP_key = GLFW.GLFW_KEY_F4;
    public static int OP_keyGoTick = GLFW.GLFW_KEY_G;
    public static int OP_interpolationAmount = 3;

    public static final Logger LOGGER = LogManager.getLogger("Maxima");
    public static MaximaClient instance;
    public static File config;
    public static File recordings = new File(MinecraftClient.getInstance().runDirectory, "maxima-recordings");

    public boolean isRecording = false;
    public boolean isSaving = false;
    public MaximaRecording recording;
    public float saveProgress;
    public boolean isWaitingForWorld = false;
    public boolean isPlayback;
    public boolean stopGeneration = false;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Loading Maxima!");

        try {
            Files.createDirectories(recordings.toPath());

            File f = FabricLoader.getInstance().getConfigDir().toFile();
            f = new File(f, "maxima.conf");
            config = f;

            if(!f.exists()) {
                LOGGER.info("Missing config! Creating default config. (First launch?)");
                this.save(f);
            } else {
                LOGGER.info("Loading config!");
                this.load(f);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Loaded Maxima!");
    }

    private void load(File config) throws FileNotFoundException, IllegalAccessException {
        Scanner scanner = new Scanner(config);

        while (scanner.hasNext()) {
            String curLine = scanner.nextLine();

            for (Field declaredField : MaximaClient.class.getDeclaredFields()) {
                if(Modifier.isStatic(declaredField.getModifiers()) && curLine.startsWith(declaredField.getName() + "=") && declaredField.getName().startsWith("OP_")) {
                    declaredField.set(null, Integer.parseInt(curLine.replace(declaredField.getName() + "=", "")));
                }
            }
        }

        scanner.close();
    }

    public void save(File config) throws IOException, IllegalAccessException {
        FileOutputStream fileOutputStream = new FileOutputStream(config);

        StringBuilder file = new StringBuilder();

        for (Field declaredField : MaximaClient.class.getDeclaredFields()) {
            if(Modifier.isStatic(declaredField.getModifiers()) && declaredField.getName().startsWith("OP_")) {
                file.append(declaredField.getName()).append("=").append(declaredField.get(null)).append("\n");
            }
        }

        fileOutputStream.write(file.toString().getBytes());
        fileOutputStream.close();
    }

    public void handleSetAngles(LivingEntity livingEntity, ModelPart leftLeg, ModelPart rightLeg, float leaningPitch, float f, float g) {
        if (MaximaClient.instance.isPlayback) {
            for (RecordingEntity entity : MaximaRecording.loadedRecording.entities.get(MaximaRecording.currentTick)) {
                if (entity.uuid.equals(livingEntity.getUuidAsString()) || UUID.fromString(entity.uuid).equals(livingEntity.getUuid())) {
                    loadCompound(leftLeg, entity.leftLeg);
                    loadCompound(rightLeg, entity.rightLeg);

                    if (entity.isPlayer && entity.leftLeg.isEmpty()) {
                        this.calculateAngles(livingEntity, leftLeg, rightLeg, leaningPitch, f, g);
                    }
                }
            }
        } else if (MaximaClient.instance.isRecording) {
            for (RecordingEntity entity : MaximaClient.instance.recording.entities.getLast()) {
                if (entity.uuid.equals(livingEntity.getUuidAsString())) {
                    entity.leftLeg = getCompound(leftLeg);
                    entity.rightLeg = getCompound(rightLeg);
                }
            }
        }
    }

    private void calculateAngles(LivingEntity livingEntity, ModelPart leftLeg, ModelPart rightLeg, float leaningPitch, float f, float g) {
        if (livingEntity.hasPlayerRider()) {
            rightLeg.pitch = -1.4137167F;
            rightLeg.yaw = 0.31415927F;
            rightLeg.roll = 0.07853982F;
            leftLeg.pitch = -1.4137167F;
            leftLeg.yaw = -0.31415927F;
            leftLeg.roll = -0.07853982F;
        }

        float k = 1.0F;
        boolean bl = livingEntity.getFallFlyingTicks() > 4;
        if (bl) {
            k = (float)livingEntity.getVelocity().lengthSquared();
            k /= 0.2F;
            k *= k * k;
        }

        if (k < 1.0F) {
            k = 1.0F;
        }

        rightLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F / k * .1f;
        leftLeg.pitch = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.4F / k * .1f;
        rightLeg.yaw = 0.005F;
        leftLeg.yaw = -0.005F;
        rightLeg.roll = 0.005F;
        leftLeg.roll = -0.005F;

        if (livingEntity.isSneaking()) {
            rightLeg.pivotZ = 4.0F;
            leftLeg.pivotZ = 4.0F;
            rightLeg.pivotY = 12.2F;
            leftLeg.pivotY = 12.2F;
        } else {
            rightLeg.pivotZ = 0.0F;
            leftLeg.pivotZ = 0.0F;
            rightLeg.pivotY = 12.0F;
            leftLeg.pivotY = 12.0F;
        }

        if (leaningPitch > 0) {
            leftLeg.pitch = MathHelper.lerp(leaningPitch, .0f, 0.2F * MathHelper.cos(f * 0.33333334F + 3.1415927F));
            rightLeg.pitch = MathHelper.lerp(leaningPitch, .0f, 0.2F * MathHelper.cos(f * 0.33333334F));
        }
    }

    @Unique
    private @NotNull NbtCompound getCompound(ModelPart part) {
        NbtCompound out = new NbtCompound();

        out.putFloat("pivotX", part.pivotX);
        out.putFloat("pivotY", part.pivotY);
        out.putFloat("pivotZ", part.pivotZ);
        out.putFloat("yaw", part.yaw);
        out.putFloat("pitch", part.pitch);
        out.putFloat("roll", part.roll);
        out.putFloat("xScale", part.xScale);
        out.putFloat("yScale", part.yScale);
        out.putFloat("zScale", part.zScale);
        out.putBoolean("visible", part.visible);
        out.putBoolean("hidden", part.hidden);

        return out;
    }

    @Unique
    private void loadCompound(ModelPart base, NbtCompound comp) {
        if (comp.isEmpty()) return;

        base.pivotX = comp.getFloat("pivotX");
        base.pivotY = comp.getFloat("pivotY");
        base.pivotZ = comp.getFloat("pivotZ");
        base.yaw = comp.getFloat("yaw");
        base.pitch = comp.getFloat("pitch");
        base.roll = comp.getFloat("roll");
        base.xScale = comp.getFloat("xScale");
        base.yScale = comp.getFloat("yScale");
        base.zScale = comp.getFloat("zScale");
        base.visible = comp.getBoolean("visible");
        base.hidden = comp.getBoolean("hidden");
    }
}
