package mx.kenzie.grammar;

public class GrammarException extends RuntimeException {
    public GrammarException() {
        super();
    }

    public GrammarException(String message) {
        super(message);
    }

    public GrammarException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrammarException(Throwable cause) {
        super(cause);
    }

    protected GrammarException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
class UnmarshallingException extends GrammarException {

    public UnmarshallingException() {
        super();
    }

    public UnmarshallingException(String message) {
        super(message);
    }

    public UnmarshallingException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnmarshallingException(Throwable cause) {
        super(cause);
    }

    protected UnmarshallingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
