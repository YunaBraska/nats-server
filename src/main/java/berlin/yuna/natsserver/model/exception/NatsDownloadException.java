package berlin.yuna.natsserver.model.exception;

public class NatsDownloadException extends RuntimeException {

    public NatsDownloadException(final String message) {
        super(message);
    }
}
