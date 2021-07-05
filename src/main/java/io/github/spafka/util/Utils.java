package io.github.spafka.util;

import io.github.spafka.tuple.Tuple2;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

@Slf4j
public class Utils {

    public <T> T timeTakeMS(Supplier<T> f, String... prefix) {
        long l = System.currentTimeMillis();
        T t = f.get();
        log.info("{} take {} ms", prefix, System.currentTimeMillis() - l);
        return t;

    }

    public <T> Tuple2<T, Long> timeTakeMS(Supplier<T> f) {
        long l = System.currentTimeMillis();
        T t = f.get();
        return new Tuple2<>(t, System.currentTimeMillis() - l);

    }

    public <T> Tuple2<Void, Long> timeTakeMS(Function0 f) {
        long l = System.currentTimeMillis();
        f.apply();
        return new Tuple2<>(null, System.currentTimeMillis() - l);

    }

    public void timeTakeMS(Function0 f, String... prefix) {
        long l = System.currentTimeMillis();
        f.apply();
        log.info("{} take {} ms", prefix, System.currentTimeMillis() - l);
    }

    public <T> void lock(Lock lock, Supplier<T> f) {
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
