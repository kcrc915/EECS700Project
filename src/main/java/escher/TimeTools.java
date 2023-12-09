package escher;

/**
 * Running time related tools
 */
public class TimeTools {
    public static class Nanosecond {
        private long value;

        public Nanosecond(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }

    public static String nanoToMillisString(Nanosecond nanosecond) {
        long millis = (long) (nanosecond.getValue() / 1e9);
        if (millis > 0) {
            double ms = nanosecond.getValue() / 1e6 - millis * 1000;
            return String.format("%d,%06.2fms", millis, ms);
        } else {
            return String.format("%.2fms", nanosecond.getValue() / 1e6);
        }
    }

    public static String nanoToSecondString(Nanosecond nanosecond) {
        return String.format("%.3fs", nanosecond.getValue() / 1e9);
    }

    public static <A> A printTimeUsed(String taskName, boolean shouldPrint, Task<A> task) {
        Tuple2<Nanosecond, A> result = measureTime(task);
        if (shouldPrint) {
            System.out.println("*** [" + taskName + "] time used: " + nanoToMillisString(result._1) + " ***");
        }
        return result._2;
    }

    public static <A> Tuple2<Nanosecond, A> measureTime(Task<A> task) {
        long t1 = System.nanoTime();
        A result = task.run();
        long time = System.nanoTime() - t1;
        return new Tuple2<>(new Nanosecond(time), result);
    }

    /**
     * Since this method uses Thread.sleep, it may not be accurate for methods with very short running time
     */
    public static <A> A scaleUpRunningTime(int factor, Task<A> task) throws InterruptedException {
        if (factor < 1) {
            throw new IllegalArgumentException("factor must be greater than or equal to 1");
        }
        if (factor == 1) {
            return task.run();
        }
        Tuple2<Nanosecond, A> result = measureTime(task);
        long extraNano = (factor - 1) * result._1.getValue();
        long millis = extraNano / 1000000;
        long nanos = extraNano - millis * 1000000;
        Thread.sleep(millis, (int) nanos);
        return result._2;
    }

    public static <A> A runOnce(Task<A> f) {
        return f.run();
    }

    public static <A> A run5Times(Task<A> f) {
        f.run();
        f.run();
        f.run();
        f.run();
        return f.run();
    }

    public interface Task<A> {
        A run();
    }

    public static class Tuple2<A, B> {
        private A _1;
        private B _2;

        public Tuple2(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        public A get_1() {
            return _1;
        }

        public void set_1(A _1) {
            this._1 = _1;
        }

        public B get_2() {
            return _2;
        }

        public void set_2(B _2) {
            this._2 = _2;
        }
    }
}


