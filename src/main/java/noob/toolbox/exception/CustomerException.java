package noob.toolbox.exception;

public class CustomerException extends RuntimeException {
    public CustomerException(String message) {
        super(message);
    }

    public CustomerException(Throwable cause) {
        super(cause);
    }
}
