/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.spafka.tuple;


/**
 * The base class of all tuples. Tuples have a fix length and contain a set of fields,
 * which may all be of different types. Because Tuples are strongly typed, each distinct
 * tuple length is represented by its own class. Tuples exists with up to 25 fields and
 * are described in the classes {@link Tuple1} to {@link Tuple10}.
 *
 * <p>The fields in the tuples may be accessed directly a public fields, or via position (zero indexed)
 * {@link #getField(int)}.
 *
 * <p>Tuples are in principle serializable. However, they may contain non-serializable fields,
 * in which case serialization will fail.
 */
public abstract class Tuple implements java.io.Serializable {

    public static final int MAX_ARITY = 10;
    private static final long serialVersionUID = 1L;
    private static final Class<?>[] CLASSES = new Class<?>[]{
            Tuple0.class, Tuple1.class, Tuple2.class, Tuple3.class, Tuple4.class, Tuple5.class, Tuple6.class, Tuple7.class, Tuple8.class, Tuple9.class, Tuple10.class
    };

    /**
     * Gets the class corresponding to the tuple of the given arity (dimensions). For
     * example, {@code getTupleClass(3)} will return the {@code Tuple3.class}.
     *
     * @param arity The arity of the tuple class to get.
     * @return The tuple class with the given arity.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Tuple> getTupleClass(int arity) {
        if (arity < 0 || arity > MAX_ARITY) {
            throw new IllegalArgumentException("The tuple arity must be in [0, " + MAX_ARITY + "].");
        }
        return (Class<? extends Tuple>) CLASSES[arity];
    }

    // BEGIN_OF_TUPLE_DEPENDENT_CODE
    // GENERATED FROM org.apache.flink.api.java.tuple.TupleGenerator.
    public static Tuple newInstance(int arity) {
        switch (arity) {
            case 0:
                return Tuple0.INSTANCE;
            case 1:
                return new Tuple1();
            case 2:
                return new Tuple2();
            case 3:
                return new Tuple3();
            case 4:
                return new Tuple4();
            case 5:
                return new Tuple5();
            case 6:
                return new Tuple6();
            case 7:
                return new Tuple7();
            case 8:
                return new Tuple8();
            case 9:
                return new Tuple9();
            case 10:
                return new Tuple10();
            default:
                throw new IllegalArgumentException("The tuple arity must be in [0, " + MAX_ARITY + "].");
        }
    }

    /**
     * Gets the field at the specified position.
     *
     * @param pos The position of the field, zero indexed.
     * @return The field at the specified position.
     * @throws IndexOutOfBoundsException Thrown, if the position is negative, or equal to, or larger than the number of fields.
     */
    public abstract <T> T getField(int pos);

    /**
     * Gets the field at the specified position, throws NullFieldException if the field is null. Used for comparing key fields.
     *
     * @param pos The position of the field, zero indexed.
     * @return The field at the specified position.
     * @throws IndexOutOfBoundsException Thrown, if the position is negative, or equal to, or larger than the number of fields.
     * @throws NullPointerException      Thrown, if the field at pos is null.
     */
    public <T> T getFieldNotNull(int pos) {
        T field = getField(pos);
        if (field != null) {
            return field;
        } else {
            throw new NullPointerException();
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Sets the field at the specified position.
     *
     * @param value The value to be assigned to the field at the specified position.
     * @param pos   The position of the field, zero indexed.
     * @throws IndexOutOfBoundsException Thrown, if the position is negative, or equal to, or larger than the number of fields.
     */
    public abstract <T> void setField(T value, int pos);

    // --------------------------------------------------------------------------------------------
    // The following lines are generated.
    // --------------------------------------------------------------------------------------------

    /**
     * Gets the number of field in the tuple (the tuple arity).
     *
     * @return The number of fields in the tuple.
     */
    public abstract int getArity();

    /**
     * Shallow tuple copy.
     *
     * @return A new Tuple with the same fields as this.
     */
    public abstract <T extends Tuple> T copy();
    // END_OF_TUPLE_DEPENDENT_CODE
}
