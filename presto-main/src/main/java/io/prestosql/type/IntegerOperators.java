/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.type;

import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import io.airlift.slice.Slice;
import io.airlift.slice.XxHash64;
import io.prestosql.spi.PrestoException;
import io.prestosql.spi.block.Block;
import io.prestosql.spi.function.BlockIndex;
import io.prestosql.spi.function.BlockPosition;
import io.prestosql.spi.function.IsNull;
import io.prestosql.spi.function.LiteralParameters;
import io.prestosql.spi.function.ScalarOperator;
import io.prestosql.spi.function.SqlNullable;
import io.prestosql.spi.function.SqlType;
import io.prestosql.spi.type.AbstractIntType;
import io.prestosql.spi.type.StandardTypes;

import static io.airlift.slice.Slices.utf8Slice;
import static io.prestosql.spi.StandardErrorCode.DIVISION_BY_ZERO;
import static io.prestosql.spi.StandardErrorCode.NUMERIC_VALUE_OUT_OF_RANGE;
import static io.prestosql.spi.function.OperatorType.ADD;
import static io.prestosql.spi.function.OperatorType.CAST;
import static io.prestosql.spi.function.OperatorType.DIVIDE;
import static io.prestosql.spi.function.OperatorType.EQUAL;
import static io.prestosql.spi.function.OperatorType.GREATER_THAN;
import static io.prestosql.spi.function.OperatorType.GREATER_THAN_OR_EQUAL;
import static io.prestosql.spi.function.OperatorType.HASH_CODE;
import static io.prestosql.spi.function.OperatorType.INDETERMINATE;
import static io.prestosql.spi.function.OperatorType.IS_DISTINCT_FROM;
import static io.prestosql.spi.function.OperatorType.LESS_THAN;
import static io.prestosql.spi.function.OperatorType.LESS_THAN_OR_EQUAL;
import static io.prestosql.spi.function.OperatorType.MODULUS;
import static io.prestosql.spi.function.OperatorType.MULTIPLY;
import static io.prestosql.spi.function.OperatorType.NEGATION;
import static io.prestosql.spi.function.OperatorType.NOT_EQUAL;
import static io.prestosql.spi.function.OperatorType.SATURATED_FLOOR_CAST;
import static io.prestosql.spi.function.OperatorType.SUBTRACT;
import static io.prestosql.spi.function.OperatorType.XX_HASH_64;
import static io.prestosql.spi.type.IntegerType.INTEGER;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.String.format;

public final class IntegerOperators
{
    private IntegerOperators() {}

    @ScalarOperator(ADD)
    @SqlType(StandardTypes.INTEGER)
    public static long add(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        try {
            return Math.addExact((int) left, (int) right);
        }
        catch (ArithmeticException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, format("integer addition overflow: %s + %s", left, right), e);
        }
    }

    @ScalarOperator(SUBTRACT)
    @SqlType(StandardTypes.INTEGER)
    public static long subtract(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        try {
            return Math.subtractExact((int) left, (int) right);
        }
        catch (ArithmeticException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, format("integer subtraction overflow: %s - %s", left, right), e);
        }
    }

    @ScalarOperator(MULTIPLY)
    @SqlType(StandardTypes.INTEGER)
    public static long multiply(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        try {
            return Math.multiplyExact((int) left, (int) right);
        }
        catch (ArithmeticException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, format("integer multiplication overflow: %s * %s", left, right), e);
        }
    }

    @ScalarOperator(DIVIDE)
    @SqlType(StandardTypes.INTEGER)
    public static long divide(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        try {
            return left / right;
        }
        catch (ArithmeticException e) {
            throw new PrestoException(DIVISION_BY_ZERO, "Division by zero", e);
        }
    }

    @ScalarOperator(MODULUS)
    @SqlType(StandardTypes.INTEGER)
    public static long modulus(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        try {
            return left % right;
        }
        catch (ArithmeticException e) {
            throw new PrestoException(DIVISION_BY_ZERO, "Division by zero", e);
        }
    }

    @ScalarOperator(NEGATION)
    @SqlType(StandardTypes.INTEGER)
    public static long negate(@SqlType(StandardTypes.INTEGER) long value)
    {
        try {
            return Math.negateExact((int) value);
        }
        catch (ArithmeticException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, "integer negation overflow: " + value, e);
        }
    }

    @ScalarOperator(EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    @SqlNullable
    public static Boolean equal(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        return left == right;
    }

    @ScalarOperator(NOT_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    @SqlNullable
    public static Boolean notEqual(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        return left != right;
    }

    @ScalarOperator(LESS_THAN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean lessThan(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        return left < right;
    }

    @ScalarOperator(LESS_THAN_OR_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean lessThanOrEqual(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        return left <= right;
    }

    @ScalarOperator(GREATER_THAN)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean greaterThan(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        return left > right;
    }

    @ScalarOperator(GREATER_THAN_OR_EQUAL)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean greaterThanOrEqual(@SqlType(StandardTypes.INTEGER) long left, @SqlType(StandardTypes.INTEGER) long right)
    {
        return left >= right;
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.BIGINT)
    public static long castToBigint(@SqlType(StandardTypes.INTEGER) long value)
    {
        return value;
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.SMALLINT)
    public static long castToSmallint(@SqlType(StandardTypes.INTEGER) long value)
    {
        try {
            return Shorts.checkedCast(value);
        }
        catch (IllegalArgumentException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for smallint: " + value, e);
        }
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.TINYINT)
    public static long castToTinyint(@SqlType(StandardTypes.INTEGER) long value)
    {
        try {
            return SignedBytes.checkedCast(value);
        }
        catch (IllegalArgumentException e) {
            throw new PrestoException(NUMERIC_VALUE_OUT_OF_RANGE, "Out of range for tinyint: " + value, e);
        }
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean castToBoolean(@SqlType(StandardTypes.INTEGER) long value)
    {
        return value != 0;
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.DOUBLE)
    public static double castToDouble(@SqlType(StandardTypes.INTEGER) long value)
    {
        return value;
    }

    @ScalarOperator(CAST)
    @SqlType(StandardTypes.REAL)
    public static long castToReal(@SqlType(StandardTypes.INTEGER) long value)
    {
        return (long) floatToRawIntBits((float) value);
    }

    @ScalarOperator(CAST)
    @LiteralParameters("x")
    @SqlType("varchar(x)")
    public static Slice castToVarchar(@SqlType(StandardTypes.INTEGER) long value)
    {
        // todo optimize me
        return utf8Slice(String.valueOf(value));
    }

    @ScalarOperator(HASH_CODE)
    @SqlType(StandardTypes.BIGINT)
    public static long hashCode(@SqlType(StandardTypes.INTEGER) long value)
    {
        return AbstractIntType.hash((int) value);
    }

    @ScalarOperator(IS_DISTINCT_FROM)
    public static final class IntegerDistinctFromOperator
    {
        @SqlType(StandardTypes.BOOLEAN)
        public static boolean isDistinctFrom(
                @SqlType(StandardTypes.INTEGER) long left,
                @IsNull boolean leftNull,
                @SqlType(StandardTypes.INTEGER) long right,
                @IsNull boolean rightNull)
        {
            if (leftNull != rightNull) {
                return true;
            }
            if (leftNull) {
                return false;
            }
            return notEqual(left, right);
        }

        @SqlType(StandardTypes.BOOLEAN)
        public static boolean isDistinctFrom(
                @BlockPosition @SqlType(value = StandardTypes.INTEGER, nativeContainerType = long.class) Block left,
                @BlockIndex int leftPosition,
                @BlockPosition @SqlType(value = StandardTypes.INTEGER, nativeContainerType = long.class) Block right,
                @BlockIndex int rightPosition)
        {
            if (left.isNull(leftPosition) != right.isNull(rightPosition)) {
                return true;
            }
            if (left.isNull(leftPosition)) {
                return false;
            }
            return notEqual(INTEGER.getLong(left, leftPosition), INTEGER.getLong(right, rightPosition));
        }
    }

    @ScalarOperator(SATURATED_FLOOR_CAST)
    @SqlType(StandardTypes.SMALLINT)
    public static long saturatedFloorCastToSmallint(@SqlType(StandardTypes.INTEGER) long value)
    {
        return Shorts.saturatedCast(value);
    }

    @ScalarOperator(SATURATED_FLOOR_CAST)
    @SqlType(StandardTypes.TINYINT)
    public static long saturatedFloorCastToTinyint(@SqlType(StandardTypes.INTEGER) long value)
    {
        return SignedBytes.saturatedCast(value);
    }

    @ScalarOperator(INDETERMINATE)
    @SqlType(StandardTypes.BOOLEAN)
    public static boolean indeterminate(@SqlType(StandardTypes.INTEGER) long value, @IsNull boolean isNull)
    {
        return isNull;
    }

    @ScalarOperator(XX_HASH_64)
    @SqlType(StandardTypes.BIGINT)
    public static long xxHash64(@SqlType(StandardTypes.INTEGER) long value)
    {
        return XxHash64.hash(value);
    }
}
