package io.github.betterclient.maxima.util.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.recording.RecordingEntity;
import io.github.betterclient.maxima.recording.RecordingWorld;
import io.github.betterclient.maxima.util.ChunkInvoker;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
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

                zos.close();
                LOGGER.info("Saved to: \"{}\"!", fileName);
                MaximaClient.instance.isSaving = false;
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }).start();
    }

    private static void saveEntities(ZipOutputStream zos) throws IOException {
        Map<Integer, byte[]> lastValues = new HashMap<>();

        int count = 0;
        for (List<RecordingEntity> entity : MaximaClient.instance.recording.entities) {
            for (RecordingEntity recordingEntity : entity) {
                if (lastValues.get(recordingEntity.entityID) != recordingEntity.data) {
                    putEntity(zos, recordingEntity, count);
                    lastValues.put(recordingEntity.entityID, recordingEntity.data);
                }
            }

            count++;
        }
    }

    private static void putEntity(ZipOutputStream zos, RecordingEntity entity, int count) throws IOException {
        zos.putNextEntry(new ZipEntry("entity/" + count + "/" + entity.uuid + ".nbt" + entity.getPText()));
        zos.write(entity.data);
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
        }
    }
}
