package io.guthub.spafka.test;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.sql.SQLOutput;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class ThreadLocals {

    static final ThreadLocal<String> namesWithInit =ThreadLocal.withInitial(()-> "van darkhome");
    static final ThreadLocal<String> namess =new ThreadLocal<>();

    static final ThreadLocal<String> namesWithInherit =new InheritableThreadLocal<>();

    @Test
    public void _1(){
        String s = namesWithInit.get();
        System.out.println();
        String s1 = namess.get();
        System.out.println(s1);
        System.out.println();

    }


    @Test
    public void inheritable() throws InterruptedException {

        namesWithInherit.set("van");

        System.out.println(namesWithInherit.get());

        Thread thread = new Thread(() -> {
            String s = namesWithInherit.get();
            System.out.println("get from parent "+s );
            namesWithInherit.set("banana");

            System.out.println();
        });
        thread.start();
        thread.join();
        String s = namesWithInherit.get();
        System.out.println("change from child "+s);

    }


    // TTL
    private static ExecutorService executorService =
            TtlExecutors.getTtlExecutorService(
                    Executors.newFixedThreadPool(2));

    private static ThreadLocal<Integer> tl =
            new TransmittableThreadLocal<>(); //这里采用TTL的实现

    @Test
    public  void main() {

        tl.set(1);

        IntStream.rangeClosed(1,1000).forEach(y->{

            executorService.execute(()->{
                int o = tl.get();
                o++;
                tl.set(o);
                System.out.println(tl.get());
            });
        });

        System.out.println(tl.get());


    }

    @Test
    public void _11(){

        IntStream.rangeClosed(1,500).forEach(x->{

            System.out.println("17767223206");
        });
    }

    @Test
    public void _12(){

        Long sd = Optional.<Long>ofNullable(1L).filter(x -> x > 0).orElseGet(() -> {

            System.out.println("sd");
            return 1L;
        });
        System.out.println(sd);
    }




}
