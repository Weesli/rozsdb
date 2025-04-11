package net.weesli.core.timeout;

import java.util.Objects;

public abstract class TimeoutTask implements Comparable<TimeoutTask> {
    private final long originalTimeOutStamp;
    private long timeoutTimestamp;
    private boolean canceled = false;

    public TimeoutTask(long timeoutInMillis) {
        this.originalTimeOutStamp = timeoutInMillis;
        this.timeoutTimestamp = System.currentTimeMillis() + timeoutInMillis;
    }
    public void run(){
        TimeoutManager.startTask(this);
    }

    public void cancel(){
        canceled = true;
        TimeoutManager.cancelTask(this);
    }

    public void reset(){
        timeoutTimestamp = System.currentTimeMillis() + originalTimeOutStamp;
        canceled = false;
    }

    public boolean isTimeoutReached(long currentTime) {
        return currentTime >= timeoutTimestamp;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public abstract void execute();

    @Override
    public int compareTo(TimeoutTask other) {
        return Long.compare(this.timeoutTimestamp, other.timeoutTimestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeoutTask that = (TimeoutTask) o;
        return timeoutTimestamp == that.timeoutTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeoutTimestamp);
    }

    public long getTimeoutTimestamp() {
        return timeoutTimestamp;
    }

}