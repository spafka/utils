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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author: spafka 2020/5/21
 * @desc :
 */
public class JoinUtils {

    public static <L, R> List<Tuple2<L, R>> innerJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return innerJoinInternal(left, right, predicate, Tuple2::new).collect(toList());
    }

    public static <L, R, U> List<U> innerJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {
        return innerJoinInternal(left, right, predicate, function).collect(toList());
    }

    public static <L, R> List<Tuple2<L, R>> leftJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate) {

        return leftJoin(left, right, predicate, (l, r) -> new Tuple2<L, R>(l, r));
    }

    public static <L, R> List<Tuple2<L, R>> rightJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate) {

        return rightJoin(left, right, predicate, Tuple2::new);
    }

    public static <L, R, U> List<U> leftJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {

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
        return sortJoinInternal(left, right, com1, com2, biCompare, function, JOINTYPE.INNER_JOIN);

    }

    public static <L, R, U> List<U> sortInnerJoin(@NonNull List<L> left,
                                                  @NonNull List<R> right,
                                                  @NonNull Comparator<L> com1,
                                                  @NonNull Comparator<R> com2,
                                                  @NonNull BiCompare<L, R> biCompare,
                                                  @NonNull BiFunction<L, R, U> function) {
        return sortJoinInternal(left, right, com1, com2, biCompare, function, JOINTYPE.RIGHT_JOIN);

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

    public static <L, R, U> List<U> rightJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {

        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(function);

        return right.stream().flatMap(x -> {
            boolean b = left.stream().anyMatch(t -> predicate.test(t, x));
            if (b) {
                return left.stream().filter(cc -> predicate.test(cc, x)).map(z -> function.apply(z, x));
            } else {
                return Stream.of(function.apply(null, x));
            }
        }).collect(toList());
    }

    public static <L, R> List<L> leftOnly(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return leftOnlyInternal(left, right, predicate, (l, r) -> l).collect(toList());
    }

    public static <L, R> List<R> rightOnly(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return rightOnlyInternal(left, right, predicate, (l, r) -> r).collect(toList());
    }

    private static <L, R, U> Stream<U> innerJoinInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(function);
        return left.stream().flatMap(x -> right.stream().filter(y -> predicate.test(x, y)).map(z -> function.apply(x, z)));
    }

    private static <L, R, U> Stream<U> rightOnlyInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {

        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(function);

        return (Stream<U>) right.stream().filter(x -> !left.stream().anyMatch(t -> predicate.test(t, x)));
    }


    private static <L, R, U> Stream<U> leftOnlyInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {

        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(function);

        return (Stream<U>) left.stream().filter(x -> !right.stream().anyMatch(t -> predicate.test(x, t)));
    }

    enum JOINTYPE {
        LEFT_JOIN, INNER_JOIN, RIGHT_JOIN
    }


    @FunctionalInterface
    public interface BiCompare<A, B> {

        int compareTo(A a, B b);
    }
}
