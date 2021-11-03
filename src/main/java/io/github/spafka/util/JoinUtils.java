/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.spafka.util;

import io.github.spafka.tuple.Tuple2;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * @author: spafka 2020/5/21
 * @desc :
 */
public class JoinUtils {


    static final int T = 1;

    public static <L, R,K> List<Tuple2<L, R>> innerJoin(List<L> left,
                                                        List<R> right,
                                                        Function<L, K> lk,
                                                        Function<R, K> rk) {
        return innerJoinInternal(left, right, lk,rk, (l, r) -> new Tuple2<L, R>(l, r)).collect(toList());
    }

    public static <L, R, K,U> List<U> innerJoin(List<L> left,
                                                List<R> right,
                                                BiFunction<L, R, U> function,
                                                Function<L, K> lk,
                                              Function<R, K> rk) {
        return innerJoinInternal(left, right, lk,rk, function).collect(toList());
    }

    public static <L, R> List<Tuple2<L, R>> leftJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate) {

        return leftJoin(left, right, predicate, (l, r) -> new Tuple2<L, R>(l, r));
    }

    public static <L, R> List<L> leftOnly(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return leftOnlyInternal(left, right, predicate, (l, r) -> l).collect(toList());
    }


    private static <L, R, K, U> Stream<U> innerJoinInternal(List<L> left,
                                                            List<R> right,
                                                            Function<L, K> lk,
                                                            Function<R, K> rk,
                                                            BiFunction<L, R, U> function) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(function);

        if (left.size() < T && right.size() < T) {
            return right.stream().flatMap(x -> left.stream().filter(y -> Objects.equals(lk.apply(y), rk.apply(x))).map(z -> function.apply(z, x)));
        } else {
            if (left.size() < right.size()) {
                Map<K, List<R>> indexMap = right.stream().collect(groupingBy(x -> rk.apply(x)));
                return left.stream()
                        .filter(x -> indexMap.containsKey(lk.apply(x)))
                        .flatMap(x -> indexMap.get(lk.apply(x)).stream().map(z -> function.apply(x, z)));
            } else {
                Map<K, List<L>> indexMap = left.stream().collect(groupingBy(x -> lk.apply(x)));
                return right.stream()
                        .filter(x -> indexMap.containsKey(rk.apply(x)))
                        .flatMap(x -> indexMap.get(rk.apply(x)).stream().map(z -> function.apply(z, x)));
            }
        }
    }

    private static <L, R, K> Stream<Tuple2<L, R>> innerJoinInternal(List<L> left,
                                                                    List<R> right,
                                                                    Function<L, K> lk,
                                                                    Function<R, K> rk) {
        return innerJoinInternal(left, right, lk, rk, (l, r) -> new Tuple2<L, R>(l, r));
    }

    private static <L, R> Stream<Tuple2<L, R>> leftJoinInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return leftJoinInternal(left, right, predicate, (l, r) -> new Tuple2<>(l, r));
    }

    private static <L, R, U> Stream<U> leftJoinInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {
        return left.stream().flatMap(x -> {
            boolean b = right.stream().anyMatch(t -> predicate.test(x, t));
            if (b) {
                return right.stream().filter(cc -> predicate.test(x, cc)).map(z -> function.apply(x, z));
            } else {
                return Stream.of(function.apply(x, null));
            }
        });
    }

    private static <L, R, U> List<U> leftJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {

        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(function);

        return left.stream().flatMap(x -> {
            boolean b = right.stream().anyMatch(t -> predicate.test(x, t));
            if (b) {
                return right.stream().filter(cc -> predicate.test(x, cc)).map(z -> function.apply(x, z));
            } else {
                return Stream.of(function.apply(x, null));
            }
        }).collect(toList());
    }


    private static <L, R, U> Stream<U> leftOnlyInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {

        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(function);

        return (Stream<U>) left.stream().filter(x -> !right.stream().anyMatch(t -> predicate.test(x, t)));
    }

    private static <L, R> Stream<L> leftOnlyInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return leftOnlyInternal(left, right, predicate, (l, r) -> l);
    }

    enum JOINTYPE {
        LEFT_JOIN, INNER_JOIN, RIGHT_JOIN
    }


    @FunctionalInterface
    public interface BiCompare<A, B> {

        int compareTo(A a, B b);
    }


    public static <L, R, U> List<U> sortLeftJoin(@NonNull List<L> left,
                                                 @NonNull List<R> right,
                                                 @NonNull Comparator<L> com1,
                                                 @NonNull Comparator<R> com2,
                                                 @NonNull BiCompare<L, R> biCompare,
                                                 @NonNull BiFunction<L, R, U> function) {
        return sortJoinInternal(left, right, com1, com2, biCompare, function, JOINTYPE.LEFT_JOIN);

    }

    public static <L, R, U> List<U> sortRightJoin(@NonNull List<L> left,
                                                  @NonNull List<R> right,
                                                  @NonNull Comparator<L> com1,
                                                  @NonNull Comparator<R> com2,
                                                  @NonNull BiCompare<L, R> biCompare,
                                                  @NonNull BiFunction<L, R, U> function) {
        return sortJoinInternal(left, right, com1, com2, biCompare, function, JOINTYPE.RIGHT_JOIN);

    }

    public static <L, R, U> List<U> sortInnerJoin(@NonNull List<L> left,
                                                  @NonNull List<R> right,
                                                  @NonNull Comparator<L> com1,
                                                  @NonNull Comparator<R> com2,
                                                  @NonNull BiCompare<L, R> biCompare,
                                                  @NonNull BiFunction<L, R, U> function) {
        return sortJoinInternal(left, right, com1, com2, biCompare, function, JOINTYPE.INNER_JOIN);

    }

    private static <L, R, U> List<U> sortJoinInternal(@NonNull List<L> left,
                                                      @NonNull List<R> right,
                                                      @NonNull Comparator<L> com1,
                                                      @NonNull Comparator<R> com2,
                                                      @NonNull BiCompare<L, R> biCompare,
                                                      @NonNull BiFunction<L, R, U> function,
                                                      @NonNull JOINTYPE jointype
    ) {


        left.sort(com1);
        right.sort(com2);

        List<U> u = new LinkedList<>();
        if (jointype == JOINTYPE.LEFT_JOIN) {
            for (L l : left) {
                boolean find = false;
                for (R r : right) {
                    int compare = biCompare.compareTo(l, r);
                    if (compare < 0) {
                        break;
                    } else if (compare == 0) {
                        find = true;
                        u.add(function.apply(l, r));
                    } else {
                    }
                }
                if (!find) {
                    if (jointype == JOINTYPE.LEFT_JOIN) {
                        u.add(function.apply(l, null));
                    }
                }
            }
        } else if (jointype == JOINTYPE.RIGHT_JOIN) {
            for (R r : right) {
                boolean find = false;
                for (L l : left) {
                    int compare = biCompare.compareTo(l, r);
                    if (compare > 0) {
                        break;
                    } else if (compare == 0) {
                        find = true;
                        u.add(function.apply(l, r));
                    } else {
                    }
                }
                if (!find) {
                    if (jointype == JOINTYPE.RIGHT_JOIN) {
                        u.add(function.apply(null, r));
                    }
                }
            }
        } else {
            for (R r : right) {
                for (L l : left) {
                    int compare = biCompare.compareTo(l, r);
                    if (compare > 0) {
                        break;
                    } else if (compare == 0) {
                        u.add(function.apply(l, r));
                    } else {
                    }
                }
            }
        }


        return u;


    }


}
