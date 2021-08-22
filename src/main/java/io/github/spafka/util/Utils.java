package io.github.spafka.util;

import io.github.spafka.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;


public class Utils {

    static final Logger log = LoggerFactory.getLogger(Utils.class);


    public static  <T> T timeTakeMS(Supplier<T> f, String... prefix) {

        long l = System.currentTimeMillis();
        T t = f.get();
        log.info("{} take {} ms", prefix, System.currentTimeMillis() - l);
        return t;

    }

    public static <T> Tuple2<T, Long> timeTakeMS(Supplier<T> f) {
        long l = System.currentTimeMillis();
        T t = f.get();
        return new Tuple2<>(t, System.currentTimeMillis() - l);

    }

    public static <T> Tuple2<Void, Long> timeTakeMS(Function0 f) {
        long l = System.currentTimeMillis();
        f.apply();
        return new Tuple2<>(null, System.currentTimeMillis() - l);

    }

    public static void timeTakeMS(Function0 f, String... prefix) {
        long l = System.currentTimeMillis();
        f.apply();
        log.info("{} take {} ms", prefix, System.currentTimeMillis() - l);
    }

    public static <T> void lock(Lock lock, Supplier<T> f) {
        try {
            lock.lock();
            f.get();
        } finally {
            lock.unlock();
        }
    }

    public interface Function0 {
        void apply();
    }

}
