package receiptprint.system;

import java.io.IOException;
import java.util.logging.*;


/// **Central Logging for an Application**
///
/// This class initializes a file-based logger (`LOG.log`) and provides
/// utility methods for:
/// * Standard logging
/// * Warning-level logging (Yellow)
/// * Error-level logging (Red)
public class Logger {

    /// Standard Java Logger
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("SystemLogger");

    /// ANSI logging colors
    public static final String RESET = "\u001B[0m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";

    // automatically init
    static {
        // initialize logger with custom write format
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
            System.err.println("Logger failed to initialize!");
            throw new RuntimeException("Logger failed to initialize!\n" + e);
        }
    }

    // Logging Methods

    /// **Standard Log**
    /// [<timestamp>] <message>
    public static void log(String msg) {
        logger.info(msg);
    }

    /// **Warning Log** (Yellow)
    /// [<timestamp>] <message>
    public static void warn(String msg) {
        logger.info(YELLOW + msg + RESET);
    }

    /// **Error Log** (Red)
    /// [<timestamp>] <message>
    public static void err(String msg) {
        logger.severe(RED + msg + RESET);
    }
}
