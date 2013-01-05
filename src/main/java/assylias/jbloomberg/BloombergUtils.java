/*
 * Copyright (C) 2012 - present by Yann Le Tallec.
 * Please see distribution for license.
 */
package assylias.jbloomberg;

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Schema;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class that should not be very useful for the users of the API.
 */
final class BloombergUtils {

    private final static Logger logger = LoggerFactory.getLogger(BloombergUtils.class);
    private static volatile boolean isBbcommStarted = false;
    private final static String BBCOMM_PROCESS = "bbcomm.exe";
    private final static String BBCOMM_FOLDER = "C:/blp/API";

    private BloombergUtils() {
    }

    /**
     * Transforms a Bloomberg Element into the most specific Object (for example: Double, Float, Integer, DateTime,
     * String etc.).
     */
    public static Object getSpecificObjectOf(Element field) {
        if (field.datatype() == Schema.Datatype.FLOAT64) {
            //likeliest data type
            return field.getValueAsFloat64();
        } else if (field.datatype() == Schema.Datatype.FLOAT32) {
            return field.getValueAsFloat32();
        } else if (field.datatype() == Schema.Datatype.BOOL) {
            return field.getValueAsBool();
        } else if (field.datatype() == Schema.Datatype.CHAR) {
            return field.getValueAsChar();
        } else if (field.datatype() == Schema.Datatype.INT32) {
            return field.getValueAsInt32();
        } else if (field.datatype() == Schema.Datatype.INT64) {
            return field.getValueAsInt64();
        } else if (field.datatype() == Schema.Datatype.DATE
                || field.datatype() == Schema.Datatype.DATETIME
                || field.datatype() == Schema.Datatype.TIME) {
            return new DateTime(field.getValueAsDate().calendar());
        } else {
            return field.getValueAsString();
        }
    }

    /**
     * Starts the bbcomm process if necessary, which is required to connect to the Bloomberg data feed.<br>
     * This method will block up to one second if it needs to manually start the process. If the process is not
     * started by the end of the timeout, this method will return false but the process might start later on.
     * <p/>
     * @return true if bbcomm was started successfully within one second, false otherwise.
     */
    public static boolean startBloombergProcessIfNecessary() {
        return isBbcommStarted || isBloombergProcessRunning() || startBloombergProcess();
    }

    /**
     *
     * @return true if the bbcomm process is running
     */
    private static boolean isBloombergProcessRunning() {
        if (ShellUtils.isProcessRunning(BBCOMM_PROCESS)) {
            logger.info("{} is started", BBCOMM_PROCESS);
            return true;
        }
        return false;
    }

    private static boolean startBloombergProcess() {
        Callable<Boolean> startBloombergProcess = getStartingCallable();
        isBbcommStarted = getResultWithTimeout(startBloombergProcess, 1, TimeUnit.SECONDS);
        return isBbcommStarted;
    }

    private static Callable<Boolean> getStartingCallable() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                logger.info("Starting {} manually", BBCOMM_PROCESS);
                ProcessBuilder pb = new ProcessBuilder(BBCOMM_PROCESS);
                pb.directory(new File(BBCOMM_FOLDER));
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("UTF-8")))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("{} > {}", BBCOMM_PROCESS, line);
                        if (line.toLowerCase().contains("started")) {
                            logger.info("{} is started", BBCOMM_PROCESS);
                            return true;
                        }
                    }
                    return false;
                }
            }
        };
    }

    private static boolean getResultWithTimeout(Callable<Boolean> startBloombergProcess, int timeout, TimeUnit timeUnit) {
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Bloomberg - bbcomm starter thread");
                t.setDaemon(true);
                return t;
            }
        });
        Future<Boolean> future = executor.submit(startBloombergProcess);

        try {
            return future.get(timeout, timeUnit);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            logger.error("Could not start bbcomm", e);
            return false;
        } finally {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    logger.warn("bbcomm starter thread still running");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
