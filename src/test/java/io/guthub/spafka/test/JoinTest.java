package io.guthub.spafka.test;

import io.github.spafka.concurrent.CompletableFutureJ9;
import io.github.spafka.tuple.Tuple2;
import io.github.spafka.util.JoinUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class JoinTest {


    static List<Integer> left = Arrays.asList(1, 2, 3, 3);
    static List<Integer> right = Arrays.asList(2, 2, 3, 4);

//            1,
//            2,2
//            2,2
//            3,3
//            3,3

    @Test
    public void _1() {

        JoinUtils.leftJoin(left, right,
                (a, b) -> a.equals(b)).forEach(System.out::println);

    }

    //    2,2
//2,2
//3,3
//3,3
//,4
    @Test
    public void _2() {

        JoinUtils.innerJoin(left, right, Function.identity(),Function.identity()).forEach(System.out::println);
    }


//
//          2,2
//          2,2
//          3,3
//          3,3

    @Test
    public void innerJoin() {
        JoinUtils.innerJoin(left, right, Function.identity(),Function.identity()).forEach(System.out::println);
    }


    //            1,
//            2,2
//            2,2
//            3,3
//            3,3
    @Test
    public void sortLeftJoin() {
        JoinUtils.sortLeftJoin(left, right, Integer::compareTo, Integer::compareTo, Integer::compareTo, Tuple2::new).forEach(System.out::println);
    }

    @Test
    public void sortRightJoin() {
        JoinUtils.sortRightJoin(left, right, Integer::compareTo, Integer::compareTo, Integer::compareTo, Tuple2::new).forEach(System.out::println);
    }

    @Test
    public void sortInnerJoin() {
        JoinUtils.sortRightJoin(left, right, Integer::compareTo, Integer::compareTo, Integer::compareTo, Tuple2::new).forEach(System.out::println);
    }


    @Test
    public void cj9() throws InterruptedException {
        CompletableFutureJ9.supplyAsync(()->{
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "11";
        }).orTimeout(1,TimeUnit.SECONDS)
                .exceptionally(x->"sss").whenComplete((r,e)->{
           if (e==null){
               System.out.println(r);
           }else {
               System.out.println(e.getCause());
           }
        });

        TimeUnit.SECONDS.sleep(22);
    }
}

