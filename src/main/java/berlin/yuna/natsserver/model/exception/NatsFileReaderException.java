package berlin.yuna.natsserver.model.exception;

public class NatsFileReaderException extends RuntimeException {

    public NatsFileReaderException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
