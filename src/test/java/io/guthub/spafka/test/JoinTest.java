package io.guthub.spafka.test;

import io.github.spafka.tuple.Tuple2;
import io.github.spafka.util.JoinUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

        JoinUtils.rightJoin(left, right, Objects::equals).forEach(System.out::println);
    }


//
//          2,2
//          2,2
//          3,3
//          3,3

    @Test
    public void innerJoin() {
        JoinUtils.innerJoin(left, right, Objects::equals).forEach(System.out::println);
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
}

