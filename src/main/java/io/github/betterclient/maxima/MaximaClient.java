package io.github.betterclient.maxima;

import io.github.betterclient.maxima.recording.MaximaRecording;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;

public class MaximaClient implements ClientModInitializer {
    public static int OP_key = GLFW.GLFW_KEY_F4;
    public static int OP_keyGoTick = GLFW.GLFW_KEY_G;

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
}
