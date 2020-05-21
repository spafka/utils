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
        return innerJoinInternal(left, right, predicate, (l, r) -> new Tuple2<L, R>(l, r)).collect(toList());
    }

    public static <L, R, U> List<U> innerJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {
        return innerJoinInternal(left, right, predicate, function).collect(toList());
    }

    public static <L, R> List<Tuple2<L, R>> leftJoin(List<L> left, List<R> right, BiPredicate<L, R> predicate) {

        return leftJoin(left, right, predicate, (l, r) -> new Tuple2<L, R>(l, r));
    }

    public static <L, R> List<L> leftOnly(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return leftOnlyInternal(left, right, predicate, (l, r) -> l).collect(toList());
    }


    private static <L, R, U> Stream<U> innerJoinInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate, BiFunction<L, R, U> function) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(function);
        return right.stream().flatMap(x -> left.stream().filter(y -> predicate.test(y, x)).map(z -> function.apply(z, x)));
    }

    private static <L, R> Stream<Tuple2<L, R>> innerJoinInternal(List<L> left, List<R> right, BiPredicate<L, R> predicate) {
        return innerJoinInternal(left, right, predicate, (l, r) -> new Tuple2<L, R>(l, r));
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


}
