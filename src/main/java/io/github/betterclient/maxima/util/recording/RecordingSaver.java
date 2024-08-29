package io.github.betterclient.maxima.util.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.type.RecordingEntity;
import io.github.betterclient.maxima.recording.type.RecordingParticle;
import io.github.betterclient.maxima.recording.type.RecordingWorld;
import io.github.betterclient.maxima.util.access.ChunkInvoker;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.github.betterclient.maxima.MaximaClient.LOGGER;

public class RecordingSaver {
    public static void saveRecording() throws IOException {
        Calendar cal = Calendar.getInstance();
        String fileName = cal.get(Calendar.DAY_OF_MONTH) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.HOUR) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND);
        File f = new File(MaximaClient.recordings, fileName + ".mxr");
        MaximaClient.instance.isSaving = true;
        MaximaClient.LOGGER.info("Saving recording...");
        new Thread(() -> {
            try {
                FileOutputStream fos = new FileOutputStream(f);
                ZipOutputStream zos = new ZipOutputStream(fos);

                saveWorlds(zos);
                saveEntities(zos);
                saveParticles(zos);

                zos.putNextEntry(new ZipEntry("minecraft.version"));
                zos.write(SharedConstants.getGameVersion().getName().getBytes());
                zos.closeEntry();

                zos.close();
                LOGGER.info("Saved to: \"{}\"!", fileName);
                MaximaClient.instance.isSaving = false;
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }).start();
    }

    private static void saveParticles(ZipOutputStream zos) throws IOException {
        int count = 0;
        for (List<RecordingParticle> particlePacket : MaximaClient.instance.recording.particlePackets) {
            int count2 = 0;
            for (RecordingParticle recordingParticle : particlePacket) {
                saveParticle(zos, recordingParticle, count, count2);
                count2++;
            }

            count++;
        }
    }

    private static void saveParticle(ZipOutputStream zos, RecordingParticle recordingParticle, int count, int count2) throws IOException {
        zos.putNextEntry(new ZipEntry("particle/" + count + "/" + count2 + ".particle"));
        zos.write(recordingParticle.data);
        zos.closeEntry();
    }

    private static void saveEntities(ZipOutputStream zos) throws IOException {
        int count = 0;
        for (List<RecordingEntity> entity : MaximaClient.instance.recording.entities) {
            for (RecordingEntity recordingEntity : entity) {
                putEntity(zos, recordingEntity, count);
            }

            count++;
        }
    }

    private static void putEntity(ZipOutputStream zos, RecordingEntity entity, int count) throws IOException {
        zos.putNextEntry(new ZipEntry("entity/" + count + "/" + entity.uuid + ".nbt" + entity.getPText()));
        zos.write(entity.appendLegs());
        zos.closeEntry();
    }

    private static void saveWorlds(ZipOutputStream zos) throws IOException {
        for (RecordingWorld world :  MaximaClient.instance.recording.worlds) {
            saveWorld(zos, world);
        }

        zos.putNextEntry(new ZipEntry("world.txt"));
        zos.write(("" +  MaximaClient.instance.recording.tickCount).getBytes());
        zos.closeEntry();
    }

    private static void saveWorld(ZipOutputStream zos, RecordingWorld world) throws IOException {
        int tc = MaximaClient.instance.recording.worlds.indexOf(world);

        for (Map.Entry<ChunkPos, ChunkData> entry : world.chunkData.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            ChunkData chunkData = entry.getValue();

            PacketByteBuf pbb = new PacketByteBuf(Unpooled.buffer());
            ((ChunkInvoker)chunkData).maxima$writeData(pbb);
            zos.putNextEntry(new ZipEntry("block/" + tc + "/" + chunkPos.x + "," + chunkPos.z + ".data"));
            zos.write(pbb.array());
            zos.closeEntry();
            pbb.release();

            pbb = new PacketByteBuf(Unpooled.buffer());
            ((ChunkInvoker)chunkData).maxima$writeHeightmap(pbb);
            zos.putNextEntry(new ZipEntry("block/" + tc + "/" + chunkPos.x + "," + chunkPos.z + ".heightmap"));
            zos.write(pbb.array());
            zos.closeEntry();
            pbb.release();

            RegistryByteBuf rbb = new RegistryByteBuf(Unpooled.buffer(), MinecraftClient.getInstance().world.getRegistryManager());
            ((ChunkInvoker)chunkData).maxima$writeBlockEntities(rbb);
            zos.putNextEntry(new ZipEntry("block/" + tc + "/" + chunkPos.x + "," + chunkPos.z + ".blockEntity"));
            zos.write(rbb.array());
            zos.closeEntry();
            rbb.release();
        }
    }
}
