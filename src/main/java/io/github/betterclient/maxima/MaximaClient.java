package io.github.betterclient.maxima;

import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.recording.type.RecordingEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;

//TODO: World time tracking
//TODO: Audio event tracking

public class MaximaClient implements ClientModInitializer {
    public static int OP_key = GLFW.GLFW_KEY_F4;
    public static int OP_keyGoTick = GLFW.GLFW_KEY_G;
    public static int OP_interpolationTick = 3;

    //Seperate the used interpolation amount from the config one.
    public static int interpolation = OP_interpolationTick;

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

        interpolation = OP_interpolationTick;

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

    public void handleSetAngles(LivingEntity livingEntity, BipedEntityModel<?> model, float leaningPitch, float f, float g, float i, float j) {
        if (MaximaClient.instance.isPlayback) {
            for (RecordingEntity entity : MaximaRecording.loadedRecording.entities.get(MaximaRecording.currentTick)) {
                if (entity.uuid.equals(livingEntity.getUuidAsString()) || UUID.fromString(entity.uuid).equals(livingEntity.getUuid())) {
                    loadAll(model, entity);

                    if (entity.isPlayer && entity.compMap.get("LEFT_LEG").comp.isEmpty()) {
                        this.calculateAngles(livingEntity, model, leaningPitch, f, g, i, j);
                    }
                }
            }
        } else if (MaximaClient.instance.isRecording) {
            for (RecordingEntity entity : MaximaClient.instance.recording.entities.getLast()) {
                if (entity.uuid.equals(livingEntity.getUuidAsString())) {
                    saveAll(model, entity);
                }
            }
        }
    }

    private void saveAll(BipedEntityModel<?> model, RecordingEntity entity) {
        entity.compMap.forEach((string, recordingPart) -> saveCompound(recordingPart.comp, recordingPart.partFunction.apply(model)));
    }

    private void loadAll(BipedEntityModel<?> model, RecordingEntity entity) {
        entity.compMap.forEach((string, recordingPart) -> loadCompound(recordingPart.partFunction.apply(model), recordingPart.comp));
    }

    private void calculateAngles(LivingEntity livingEntity, BipedEntityModel<?> model, float leaningPitch, float f, float g, float i, float j) {
        if (livingEntity.hasPlayerRider()) {
            model.rightLeg.pitch = -1.4137167F;
            model.rightLeg.yaw = 0.31415927F;
            model.rightLeg.roll = 0.07853982F;
            model.leftLeg.pitch = -1.4137167F;
            model.leftLeg.yaw = -0.31415927F;
            model.leftLeg.roll = -0.07853982F;
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

        model.rightLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * .1f / k;
        model.leftLeg.pitch = MathHelper.cos((float) (f * 0.6662F + Math.PI)) * 1.4F * .1f / k;
        model.rightLeg.yaw = 0.005F;
        model.leftLeg.yaw = -0.005F;
        model.rightLeg.roll = 0.005F;
        model.leftLeg.roll = -0.005F;

        if (livingEntity.isSneaking()) {
            model.rightLeg.pivotZ = 4.0F;
            model.leftLeg.pivotZ = 4.0F;
            model.rightLeg.pivotY = 12.2F;
            model.leftLeg.pivotY = 12.2F;
        } else {
            model.rightLeg.pivotZ = 0.0F;
            model.leftLeg.pivotZ = 0.0F;
            model.rightLeg.pivotY = 12.0F;
            model.leftLeg.pivotY = 12.0F;
        }

        if (leaningPitch > 0) {
            model.leftLeg.pitch = MathHelper.lerp(leaningPitch, model.leftLeg.pitch, 0.3F * MathHelper.cos(f * 0.33333334F + 3.1415927F));
            model.rightLeg.pitch = MathHelper.lerp(leaningPitch, model.rightLeg.pitch, 0.3F * MathHelper.cos(f * 0.33333334F));
        }

        ModelPart head = model.head;
        ModelPart body = model.body;

        boolean bl2 = livingEntity.isInSwimmingPose();
        head.yaw = i * 0.017453292F;
        if (bl) {
            head.pitch = -0.7853982F;
        } else if (leaningPitch > 0.0F) {
            if (bl2) {
                head.pitch = this.lerpAngle(leaningPitch, head.pitch, -0.7853982F);
            } else {
                head.pitch = this.lerpAngle(leaningPitch, head.pitch, j * 0.017453292F);
            }
        } else {
            head.pitch = j * 0.017453292F;
        }

        body.yaw = 0.0F;

        boolean found = false;
        for (RecordingEntity recordingEntity : MaximaRecording.loadedRecording.entities.get(MaximaRecording.currentTick)) {
            if (recordingEntity.uuid.equals(livingEntity.getUuidAsString())) {
                try {
                    if (recordingEntity.generate().getBoolean("SNEAKING")) {
                        found = true;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (!found) {
            model.body.pitch = 0.0F;
            model.rightLeg.pivotZ = 0.0F;
            model.leftLeg.pivotZ = 0.0F;
            model.rightLeg.pivotY = 12.0F;
            model.leftLeg.pivotY = 12.0F;
            model.head.pivotY = 0.0F;
            model.body.pivotY = 0.0F;
            model.leftArm.pivotY = 2.0F;
            model.rightArm.pivotY = 2.0F;
        } else {
            model.body.pitch = 0.5F;
            ModelPart var10000 = model.rightArm;
            var10000.pitch += 0.4F;
            var10000 = model.leftArm;
            var10000.pitch += 0.4F;
            model.rightLeg.pivotZ = 4.0F;
            model.leftLeg.pivotZ = 4.0F;
            model.rightLeg.pivotY = 12.2F;
            model.leftLeg.pivotY = 12.2F;
            model.head.pivotY = 4.2F;
            model.body.pivotY = 3.2F;
            model.leftArm.pivotY = 5.2F;
            model.rightArm.pivotY = 5.2F;
        }
    }

    protected float lerpAngle(float angleOne, float angleTwo, float magnitude) {
        float f = (magnitude - angleTwo) % 6.2831855F;
        if (f < -3.1415927F) {
            f += 6.2831855F;
        }

        if (f >= 3.1415927F) {
            f -= 6.2831855F;
        }

        return angleTwo + angleOne * f;
    }

    private void saveCompound(NbtCompound out, ModelPart part) {
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
    }

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
