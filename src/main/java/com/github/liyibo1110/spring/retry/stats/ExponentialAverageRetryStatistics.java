package com.github.liyibo1110.spring.retry.stats;

/**
 * 基于指数移动平均的统计器，用O(1)的方式，近似反映最近一段时间的行为趋势
 * @author liyibo
 * @date 2026-01-31 20:25
 */
public class ExponentialAverageRetryStatistics extends  DefaultRetryStatistics {
    private long window = 15000;
    private ExponentialAverage started;
    private ExponentialAverage error;
    private ExponentialAverage complete;
    private ExponentialAverage recovery;
    private ExponentialAverage abort;

    public ExponentialAverageRetryStatistics(String name) {
        super(name);
        this.init();
    }

    private void init() {
        this.started = new ExponentialAverage(window);
        this.error = new ExponentialAverage(window);
        this.complete = new ExponentialAverage(window);
        this.abort = new ExponentialAverage(window);
        this.recovery = new ExponentialAverage(window);
    }

    public void setWindow(long window) {
        this.window = window;
        this.init();    // window值变了，则要重新生成各统计器
    }

    public int getRollingStartedCount() {
        return (int)Math.round(started.getValue());
    }

    public int getRollingErrorCount() {
        return (int)Math.round(error.getValue());
    }

    public int getRollingAbortCount() {
        return (int)Math.round(abort.getValue());
    }

    public int getRollingRecoveryCount() {
        return (int) Math.round(recovery.getValue());
    }

    public int getRollingCompleteCount() {
        return (int)Math.round(complete.getValue());
    }

    public double getRollingErrorRate() {
        if(Math.round(started.getValue()) == 0)
            return 0.;
        return (abort.getValue() + recovery.getValue()) / started.getValue();
    }

    @Override
    public void incrementStartedCount() {
        super.incrementStartedCount();
        this.started.increment();
    }

    @Override
    public void incrementCompleteCount() {
        super.incrementCompleteCount();
        this.complete.increment();
    }

    @Override
    public void incrementRecoveryCount() {
        super.incrementRecoveryCount();
        this.recovery.increment();
    }

    @Override
    public void incrementErrorCount() {
        super.incrementErrorCount();
        this.error.increment();
    }

    @Override
    public void incrementAbortCount() {
        super.incrementAbortCount();
        this.abort.increment();
    }

    private class ExponentialAverage {
        private final double alpha;
        private volatile long lastTime = System.currentTimeMillis();
        /** 最近一段时间内的频率密度 */
        private volatile double value = 0;

        public ExponentialAverage(long window) {
            alpha = 1. / window;
        }

        /**
         * 标准的EMA计算公式
         */
        public synchronized void increment() {
            long time = System.currentTimeMillis();
            value = value * Math.exp(-alpha * (time - lastTime)) + 1;
            lastTime = time;
        }

        public double getValue() {
            long time = System.currentTimeMillis();
            // 查询也会降低value的值
            return value * Math.exp(-alpha * (time - lastTime));
        }
    }
}
