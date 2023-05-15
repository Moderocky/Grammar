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
