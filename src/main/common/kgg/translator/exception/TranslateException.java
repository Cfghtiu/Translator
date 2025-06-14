package kgg.translator.exception;

import java.io.IOException;

public class TranslateException extends IOException {
    public TranslateException() {
    }

    public TranslateException(String message) {
        super(message);
    }

    public TranslateException(Throwable cause) {
        super(cause);
    }

    public TranslateException(String message, Throwable cause) {
        super(message, cause);
    }
}
