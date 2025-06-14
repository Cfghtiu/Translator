package kgg.translator.translator;

import com.google.gson.JsonObject;
import kgg.translator.TranslatorConfig;
import kgg.translator.TranslatorManager;
import kgg.translator.command.CommandConfigurable;
import kgg.translator.exception.TranslateException;
import kgg.translator.ocrtrans.ResRegion;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Translator implements CommandConfigurable {
    private boolean configured = false;  // 是否已经配置

    public String translate(String text, String from, String to, String source) throws IOException {
        return translate(text, from, to);
    }

    protected String translate(String text, String from, String to) throws IOException {
        throw new NotImplementedException();
    };

    public ResRegion[] ocrtrans(byte[] img, String from, String to) throws IOException {
        throw new TranslateException(getName() + "不支持图片翻译");
    }

    public abstract String getName();

    public String getLanguageType() {
        return getName();
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured() {
        setConfigured(true);
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
//        if (configured) {
//            if (!TranslatorConfig.isInit()) {
//                TranslatorConfig.writeFile();
//            }
//        }
    }

    public abstract void read(JsonObject object);
    public abstract void write(JsonObject object);

    @Override
    public String toString() {
        return getName();
    }

    private long startTime = 0;
    private final Lock lock = new ReentrantLock();
    protected <T> T delay(long time, ThrowingSupplier<T> runnable) throws IOException {
        lock.lock();
        if (System.currentTimeMillis() - startTime < time) {
            try {
                TimeUnit.MILLISECONDS.sleep(time - (System.currentTimeMillis() - startTime));
            } catch (InterruptedException ignored) {}
        }
        try {
            return runnable.get();
        } finally {
            startTime = System.currentTimeMillis();
            lock.unlock();
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws IOException;
    }
}
