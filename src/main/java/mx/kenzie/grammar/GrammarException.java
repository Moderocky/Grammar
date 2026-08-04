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


    public static class NoUnmarshallingStrategy extends StrategyException {

        public NoUnmarshallingStrategy(String message) {
            super(message);
        }

    }


    public static class NoMarshallingStrategy extends StrategyException {

        public NoMarshallingStrategy(String message) {
            super(message);
        }

    }

    public static class UnmarshallingException extends GrammarException {

        public UnmarshallingException(String message) {
            super(message);
        }

        public UnmarshallingException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnmarshallingException(Throwable cause) {
            super(cause);
        }

    }

    public static class StrategyException extends GrammarException {

        public StrategyException() {
        }

        public StrategyException(String message, Throwable cause) {
            super(message, cause);
        }

        public StrategyException(String message) {
            super(message);
        }
    }

    public static class ReadingException extends GrammarException {
        public ReadingException(String message) {
        }
    }
}