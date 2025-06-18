package kgg.translator.handler;

import kgg.translator.TranslateService;
import kgg.translator.ocrtrans.ResRegion;
import kgg.translator.screen.OcrScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.stb.STBImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class OcrHandler {
    private static final Logger LOGGER = LogManager.getLogger(OcrHandler.class);

    public static void start() {
        MinecraftClient client = MinecraftClient.getInstance();
        OcrScreen screen = new OcrScreen(client.currentScreen);
        client.setScreen(screen);
        try (NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())){
            byte[] bytes = getBytes(nativeImage);
            CompletableFuture.runAsync(() -> {
                try {
                    ResRegion[] ocrtrans = TranslateService.ocrtrans(bytes);
                    // 每个区域进行缩放
                    ocrtrans = Arrays.stream(ocrtrans).map(resRegion -> {
                        return resRegion.scale(1 / client.getWindow().getScaleFactor());
                    }).toArray(ResRegion[]::new);
                    screen.setResRegions(ocrtrans);
                } catch (Exception e) {
                    screen.setError(e.getMessage());
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    public static byte[] getBytes(NativeImage nativeImage) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] var3;
        try {
            WritableByteChannel writableByteChannel = Channels.newChannel(byteArrayOutputStream);

            try {
                if (!nativeImage.write(writableByteChannel)) {
                    throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
                }

                var3 = byteArrayOutputStream.toByteArray();
            } catch (Throwable var7) {
                if (writableByteChannel != null) {
                    try {
                        writableByteChannel.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (writableByteChannel != null) {
                writableByteChannel.close();
            }
        } catch (Throwable var8) {
            try {
                byteArrayOutputStream.close();
            } catch (Throwable var5) {
                var8.addSuppressed(var5);
            }

            throw var8;
        }

        byteArrayOutputStream.close();
        return var3;
    }
}
