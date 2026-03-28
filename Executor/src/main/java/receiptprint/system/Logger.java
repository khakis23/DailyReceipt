package receiptprint.system;

import java.io.IOException;
import java.util.logging.*;

public class Logger {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("SystemLogger");

    // ANSI logging colors
    public static final String RESET = "\u001B[0m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";

    // automatically init
    static {
        try {
            FileHandler fh = new FileHandler("LOG.log", true);
            fh.setFormatter(
                    // custom log format:  [<timestamp>] <message>
                    new Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            return String.format("[%1$tF %1$tT] %2$s%n",
                                    record.getMillis(),
                                    record.getMessage());
                        }
                    }
            );

            logger.addHandler(fh);
            logger.setUseParentHandlers(false); // Disable normal console output
            Logger.log("Logger Initialized!");

        } catch (IOException e) {
            throw new RuntimeException("Logger failed to initialize!\n" + e);
        }
    }

    public static void log(String msg) {
        logger.info(msg);
    }

    public static void warn(String msg) {
        logger.info(YELLOW + msg + RESET);
    }

    public static void err(String msg) {
        logger.severe(RED + msg + RESET);
    }

}
