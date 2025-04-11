package net.weesli.core.timeout;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeoutManager {
        private static final ConcurrentSkipListSet<TimeoutTask> tasks = new ConcurrentSkipListSet<>();
        private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        private static final ExecutorService executor = Executors.newFixedThreadPool(1);
        private static final AtomicBoolean isRunning = new AtomicBoolean(false);

        static {
            scheduler.scheduleAtFixedRate(TimeoutManager::checkAndRunTasks, 0, 1, TimeUnit.SECONDS);
        }

        private static void checkAndRunTasks() {
            if (!isRunning.compareAndSet(false, true)) {
                return;
            }

            try {
                executor.execute(() -> {
                    try {
                        long now = System.currentTimeMillis();
                        Iterator<TimeoutTask> iterator = tasks.iterator();
                        while (iterator.hasNext()) {
                            TimeoutTask task = iterator.next();
                            if (task != null) {
                                if (task.isTimeoutReached(now)) {
                                    iterator.remove();
                                    task.execute();
                                }
                            }
                        }
                    } finally {
                        isRunning.set(false);
                    }
                });
            } catch (Exception e) {
                isRunning.set(false);
            }
        }

        public static void startTask(TimeoutTask task) {
            tasks.add(task);
        }

        public static void cancelTask(TimeoutTask cleanerTask) {
            tasks.remove(cleanerTask);
        }

        public static void shutdown() {
            scheduler.shutdown();
            executor.shutdown();
        }
}
