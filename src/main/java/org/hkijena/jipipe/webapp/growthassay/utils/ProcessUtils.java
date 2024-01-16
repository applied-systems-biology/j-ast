package org.hkijena.jipipe.webapp.growthassay.utils;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.WordUtils;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ProcessUtils {

    /**
     * Queries standard output with a timeout.
     * Does not listen to cancellation signals
     *
     * @param executable   the executable
     * @param progressInfo the progress info
     * @param args         executable args
     * @return the stdout
     */
    public static String queryFast(Path executable, ProgressInfo progressInfo, String... args) {
        CommandLine commandLine = new CommandLine(executable.toFile());
        commandLine.addArguments(args);
        progressInfo.log("Running " + executable + " " + String.join(" ", args));
        DefaultExecutor executor = new DefaultExecutor();

        // Capture stdout
        ByteArrayOutputStream standardOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutputStream = new ByteArrayOutputStream();
        PumpStreamHandler outputStreamHandler = new PumpStreamHandler(standardOutputStream, errorOutputStream);
        executor.setStreamHandler(outputStreamHandler);

        try {
            int exitValue = executor.execute(commandLine);

            if (!executor.isFailure(exitValue)) {
                return standardOutputStream.toString();
            } else {
                return null;
            }

        } catch (IOException e) {
            return null;
        }
    }

    public static void setupLogger(CommandLine commandLine, DefaultExecutor executor, ProgressInfo progressInfo) {
        progressInfo.log("Running " + commandLine.toString());

        LogOutputStream progressInfoLog = new LogOutputStream() {
            @Override
            protected void processLine(String s, int i) {
                for (String s1 : s.split("\\r")) {
                    progressInfo.log(WordUtils.wrap(s1, 120));
                }
            }
        };
        executor.setStreamHandler(new PumpStreamHandler(progressInfoLog, progressInfoLog));
    }

    public static void killProcessTree(long pid, ProgressInfo progressInfo) {
        if (pid != -1) {
            progressInfo.log("Cancelling process tree rooted at PID " + pid);
            if (SystemUtils.IS_OS_WINDOWS) {
                // Windows uses taskkill
                progressInfo.log(queryFast(Paths.get(System.getenv("WINDIR")).resolve("System32").resolve("taskkill"),
                        progressInfo,
                        "/F", "/PID", pid + "", "/T"));
            } else {
                // Unix provides pkill
                String psPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new ProgressInfo(), "ps")).trim();
                String killPath = StringUtils.nullToEmpty(ProcessUtils.queryFast(Paths.get("/usr/bin/which"), new ProgressInfo(), "kill")).trim();
                if (!StringUtils.isNullOrEmpty(psPath)) {
                    String psOutput = queryFast(Paths.get(psPath),
                            progressInfo,
                            "-A", "-o", "pid,ppid");
                    if (!StringUtils.isNullOrEmpty(psOutput)) {
                        psOutput = psOutput.trim();
                        DefaultDirectedGraph<Long, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
                        graph.addVertex(pid);
                        for (String line : psOutput.split("\n")) {
                            String line_ = line.trim();
                            if (line_.startsWith("P"))
                                continue;
                            String[] components = line_.split("\\s+");
                            long psPid = Long.parseLong(components[0]);
                            long psParentPid = Long.parseLong(components[1]);
                            if (!graph.containsVertex(psPid))
                                graph.addVertex(psPid);
                            if (psParentPid > 0) {
                                if (!graph.containsVertex(psParentPid))
                                    graph.addVertex(psParentPid);
                                graph.addEdge(psParentPid, psPid);
                            }
                        }

                        // List all children
                        try {
                            BreadthFirstIterator<Long, DefaultEdge> breadthFirstIterator = new BreadthFirstIterator<>(graph, pid);
                            while (breadthFirstIterator.hasNext()) {
                                long toKill = breadthFirstIterator.next();
                                progressInfo.log("Killing orphaned PID " + toKill);
                                queryFast(Paths.get(killPath), progressInfo, "-9", toKill + "");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    progressInfo.log("Error: Could not find pkill.");
                }
            }
        } else {
            progressInfo.log("Error: PID is -1. Cannot cancel process tree.");
        }
    }

    public static class ExtendedExecutor extends DefaultExecutor {

        private final ProgressInfo progressInfo;
        private ProcessTree process;

        public ExtendedExecutor(long timeout, ProgressInfo progressInfo, Path lockFilePath) {
            super();
            this.progressInfo = progressInfo;
            setWatchdog(new RunCancellationExecuteWatchdog(timeout, progressInfo, lockFilePath, this));
        }

        @Override
        protected Process launch(CommandLine command, Map<String, String> env, File dir) throws IOException {
            process = new ProcessTree(super.launch(command, env, dir), progressInfo);
            return process;
        }

        public long getPid() {
            return process.getPid();
        }

        public ProgressInfo getProgressInfo() {
            return progressInfo;
        }

        public ProcessTree getProcess() {
            return process;
        }
    }

    /**
     * Wrapper around an existing process that models a process tree
     */
    public static class ProcessTree extends Process {
        private final Process process;
        private final long pid;
        private final ProgressInfo progressInfo;

        public ProcessTree(Process process, ProgressInfo progressInfo) {
            this.process = process;
            this.pid = process.pid();
            this.progressInfo = progressInfo;
        }

        public Process getProcess() {
            return process;
        }

        public long getPid() {
            return pid;
        }


        @Override
        public OutputStream getOutputStream() {
            return process.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return process.getInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return process.getErrorStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }

        @Override
        public int exitValue() {
            return process.exitValue();
        }

        @Override
        public void destroy() {
            killProcessTree(pid, progressInfo);
        }

        public ProgressInfo getProgressInfo() {
            return progressInfo;
        }
    }

    /**
     * Based on {@link ExecuteWatchdog}. Adapted to listed to {@link ProgressInfo} cancellation.
     */
    public static class RunCancellationExecuteWatchdog extends ExecuteWatchdog {

        private final ProgressInfo progressInfo;
        private final RunCancellationWatchdog cancellationWatchdog;
        private final ExtendedExecutor extendedExecutor;

        /**
         * Creates a new watchdog with a given timeout.
         *
         * @param timeout          the timeout for the process in milliseconds. It must be
         *                         greater than 0 or 'INFINITE_TIMEOUT'
         * @param lockFilePath  the lockfile (must exist)
         * @param extendedExecutor the executor
         */
        public RunCancellationExecuteWatchdog(long timeout, ProgressInfo progressInfo, Path lockFilePath, ExtendedExecutor extendedExecutor) {
            super(timeout);
            this.progressInfo = progressInfo;
            this.cancellationWatchdog = new RunCancellationWatchdog(progressInfo, lockFilePath);
            this.extendedExecutor = extendedExecutor;
            this.cancellationWatchdog.getEventBus().register(this);
        }

        public ExtendedExecutor getExtendedExecutor() {
            return extendedExecutor;
        }

        @Override
        public synchronized void timeoutOccured(Watchdog w) {
            // Kill the process using the PID
            long pid = extendedExecutor.getPid();
            killProcessTree(pid, progressInfo);
            super.timeoutOccured(w);
        }

        @Subscribe
        public synchronized void onProcessCancelled(RunCancellationWatchdog.CancelledEvent event) {
            this.timeoutOccured(null);
        }

        @Override
        public synchronized void start(Process processToMonitor) {
            super.start(processToMonitor);
            cancellationWatchdog.start();
        }

        @Override
        public synchronized void stop() {
            cancellationWatchdog.stop();
            super.stop();
        }
    }

    /**
     * A watchdog that monitors a sub-process of a {@link Runnable}
     * and watches for the {@link Runnable} to be cancelled.
     * Based on {@link Watchdog}
     */
    public static class RunCancellationWatchdog implements Runnable {
        private final EventBus eventBus = new EventBus();
        private final ProgressInfo progressInfo;
        private final Path lockFilePath;
        private boolean stopped = false;

        public RunCancellationWatchdog(ProgressInfo progressInfo, Path lockFilePath) {
            this.progressInfo = progressInfo;
            this.lockFilePath = lockFilePath;
        }

        public synchronized void start() {
            stopped = false;
            final Thread t = new Thread(this, "WATCHDOG");
            t.setDaemon(true);
            t.start();
        }

        public synchronized void stop() {
            stopped = true;
            notifyAll();
        }

        public void run() {
            boolean isWaiting;
            synchronized (this) {
                isWaiting = true;
                while (!stopped && isWaiting) {
                    try {
                        wait(500);
                    } catch (final InterruptedException e) {
                    }
                    isWaiting = !progressInfo.isCancelled() && Files.isRegularFile(lockFilePath);
                }
            }

            // notify the listeners outside the synchronized block (see EXEC-60)
            if (!isWaiting) {
                eventBus.post(new CancelledEvent(this));
            }
        }

        public EventBus getEventBus() {
            return eventBus;
        }

        public static class CancelledEvent {
            private final RunCancellationWatchdog watchdog;

            public CancelledEvent(RunCancellationWatchdog watchdog) {
                this.watchdog = watchdog;
            }

            public RunCancellationWatchdog getWatchdog() {
                return watchdog;
            }
        }
    }
}
