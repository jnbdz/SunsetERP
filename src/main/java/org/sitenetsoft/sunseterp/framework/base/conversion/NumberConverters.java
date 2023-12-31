/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.sitenetsoft.sunseterp.framework.base.conversion;

import org.sitenetsoft.sunseterp.framework.base.util.StringUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

/** Number Converter classes. */
public class NumberConverters implements ConverterLoader {

    private static Number fromString(String str, Locale locale) throws ConversionException {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        if (nf instanceof DecimalFormat) {
            // CHECKSTYLE_OFF: ALMOST_ALL
            ((DecimalFormat) nf).setParseBigDecimal(true);
            // CHECKSTYLE_ON: ALMOST_ALL
        }
        try {
            return nf.parse(str);
        } catch (ParseException e) {
            throw new ConversionException(e);
        }
    }

    public abstract static class AbstractStringToNumberConverter<N extends Number> extends AbstractNumberConverter<String, N> {
        AbstractStringToNumberConverter(Class<N> targetClass) {
            super(String.class, targetClass);
        }

        @Override
        public N convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            String trimStr = StringUtil.removeSpaces(obj);
            if (trimStr.isEmpty()) {
                return null;
            }
            return convert(fromString(trimStr, locale));
        }

        protected abstract N convert(Number number) throws ConversionException;
    }

    public abstract static class AbstractNumberConverter<S, T> extends AbstractLocalizedConverter<S, T> {
        AbstractNumberConverter(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

        @Override
        public T convert(S obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, locale, null);
        }
    }

    public abstract static class AbstractNumberToStringConverter<N extends Number> extends AbstractNumberConverter<N, String> {
        AbstractNumberToStringConverter(Class<N> sourceClass) {
            super(sourceClass, String.class);
        }

        @Override
        public String convert(N obj) throws ConversionException {
            return obj.toString();
        }

        @Override
        public String convert(N obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return format(obj, NumberFormat.getNumberInstance(locale));
        }

        protected abstract String format(N obj, NumberFormat nf) throws ConversionException;
    }

    public static class GenericNumberToDouble<N extends Number> extends AbstractConverter<N, Double> {
        GenericNumberToDouble(Class<N> sourceClass) {
            super(sourceClass, Double.class);
        }

        @Override
        public Double convert(N obj) throws ConversionException {
            return obj.doubleValue();
        }
    }

    public static class GenericNumberToFloat<N extends Number> extends AbstractConverter<N, Float> {
        GenericNumberToFloat(Class<N> sourceClass) {
            super(sourceClass, Float.class);
        }

        @Override
        public Float convert(N obj) throws ConversionException {
            return obj.floatValue();
        }
    }

    public static class GenericNumberToInteger<N extends Number> extends AbstractConverter<N, Integer> {
        GenericNumberToInteger(Class<N> sourceClass) {
            super(sourceClass, Integer.class);
        }

        @Override
        public Integer convert(N obj) throws ConversionException {
            return obj.intValue();
        }
    }

    public static class GenericNumberToLong<N extends Number> extends AbstractConverter<N, Long> {
        GenericNumberToLong(Class<N> sourceClass) {
            super(sourceClass, Long.class);
        }

        @Override
        public Long convert(N obj) throws ConversionException {
            return obj.longValue();
        }
    }

    public static class GenericNumberToShort<N extends Number> extends AbstractConverter<N, Short> {
        GenericNumberToShort(Class<N> sourceClass) {
            super(sourceClass, Short.class);
        }

        @Override
        public Short convert(N obj) throws ConversionException {
            return obj.shortValue();
        }
    }

    public static class BigDecimalToString extends AbstractNumberToStringConverter<BigDecimal> {
        public BigDecimalToString() {
            super(BigDecimal.class);
        }

        @Override
        protected String format(BigDecimal obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.doubleValue());
        }
    }

    public static class DoubleToBigDecimal extends AbstractConverter<Double, BigDecimal> {
        public DoubleToBigDecimal() {
            super(Double.class, BigDecimal.class);
        }

        @Override
        public BigDecimal convert(Double obj) throws ConversionException {
            return BigDecimal.valueOf(obj);
        }
    }

    public static class BigIntegerToString extends AbstractNumberToStringConverter<BigInteger> {
        public BigIntegerToString() {
            super(BigInteger.class);
        }

        @Override
        protected String format(BigInteger obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.doubleValue());
        }
    }

    public static class IntegerToBigInteger extends AbstractConverter<Integer, BigInteger> {
        public IntegerToBigInteger() {
            super(Integer.class, BigInteger.class);
        }

        @Override
        public BigInteger convert(Integer obj) throws ConversionException {
            return BigInteger.valueOf(obj.intValue());
        }
    }

    public static class ByteToString extends AbstractNumberToStringConverter<Byte> {
        public ByteToString() {
            super(Byte.class);
        }

        @Override
        protected String format(Byte obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.floatValue());
        }
    }

    public static class StringToBigInteger extends AbstractStringToNumberConverter<BigInteger> {
        public StringToBigInteger() {
            super(BigInteger.class);
        }

        @Override
        public BigInteger convert(String obj) throws ConversionException {
            return new BigInteger(obj);
        }

        @Override
        protected BigInteger convert(Number number) throws ConversionException {
            return BigInteger.valueOf(number.longValue());
        }
    }

    public static class DoubleToString extends AbstractNumberToStringConverter<Double> {
        public DoubleToString() {
            super(Double.class);
        }

        @Override
        protected String format(Double obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.doubleValue());
        }
    }

    public static class FloatToBigDecimal extends AbstractConverter<Float, BigDecimal> {
        public FloatToBigDecimal() {
            super(Float.class, BigDecimal.class);
        }

        @Override
        public BigDecimal convert(Float obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }
    }

    public static class FloatToString extends AbstractNumberToStringConverter<Float> {
        public FloatToString() {
            super(Float.class);
        }

        @Override
        protected String format(Float obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.floatValue());
        }
    }

    public static class IntegerToBigDecimal extends AbstractConverter<Integer, BigDecimal> {
        public IntegerToBigDecimal() {
            super(Integer.class, BigDecimal.class);
        }

        @Override
        public BigDecimal convert(Integer obj) throws ConversionException {
            return BigDecimal.valueOf(obj);
        }
    }

    public static class IntegerToByte extends AbstractConverter<Integer, Byte> {
        public IntegerToByte() {
            super(Integer.class, Byte.class);
        }

        @Override
        public Byte convert(Integer obj) throws ConversionException {
            return obj.byteValue();
        }
    }

    public static class IntegerToString extends AbstractNumberToStringConverter<Integer> {
        public IntegerToString() {
            super(Integer.class);
        }

        @Override
        protected String format(Integer obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.intValue());
        }
    }

    public static class LongToBigDecimal extends AbstractConverter<Long, BigDecimal> {
        public LongToBigDecimal() {
            super(Long.class, BigDecimal.class);
        }

        @Override
        public BigDecimal convert(Long obj) throws ConversionException {
            return BigDecimal.valueOf(obj);
        }
    }

    public static class LongToByte extends AbstractConverter<Long, Byte> {
        public LongToByte() {
            super(Long.class, Byte.class);
        }

        @Override
        public Byte convert(Long obj) throws ConversionException {
            return obj.byteValue();
        }
    }

    public static class LongToString extends AbstractNumberToStringConverter<Long> {
        public LongToString() {
            super(Long.class);
        }

        @Override
        protected String format(Long obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.longValue());
        }
    }

    public static class ShortToString extends AbstractNumberToStringConverter<Short> {
        public ShortToString() {
            super(Short.class);
        }

        @Override
        protected String format(Short obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.floatValue());
        }
    }

    public static class StringToBigDecimal extends AbstractStringToNumberConverter<BigDecimal> {
        public StringToBigDecimal() {
            super(BigDecimal.class);
        }

        @Override
        public BigDecimal convert(String obj) throws ConversionException {
            return new BigDecimal(obj);
        }

        @Override
        protected BigDecimal convert(Number number) throws ConversionException {
            if (number instanceof BigDecimal) {
                return (BigDecimal) number;
            }
            return BigDecimal.valueOf(number.doubleValue());
        }
    }

    public static class StringToByte extends AbstractConverter<String, Byte> {
        public StringToByte() {
            super(String.class, Byte.class);
        }

        @Override
        public Byte convert(String obj) throws ConversionException {
            return Byte.valueOf(obj);
        }
    }

    public static class StringToDouble extends AbstractStringToNumberConverter<Double> {
        public StringToDouble() {
            super(Double.class);
        }

        @Override
        public Double convert(String obj) throws ConversionException {
            return Double.valueOf(obj);
        }

        @Override
        protected Double convert(Number number) throws ConversionException {
            return number.doubleValue();
        }
    }

    public static class StringToFloat extends AbstractStringToNumberConverter<Float> {
        public StringToFloat() {
            super(Float.class);
        }

        @Override
        public Float convert(String obj) throws ConversionException {
            return Float.valueOf(obj);
        }

        @Override
        protected Float convert(Number number) throws ConversionException {
            return number.floatValue();
        }
    }

    public static class StringToInteger extends AbstractStringToNumberConverter<Integer> {
        public StringToInteger() {
            super(Integer.class);
        }

        @Override
        public Integer convert(String obj) throws ConversionException {
            return Integer.valueOf(obj);
        }

        @Override
        protected Integer convert(Number number) throws ConversionException {
            return number.intValue();
        }
    }

    public static class StringToLong extends AbstractStringToNumberConverter<Long> {
        public StringToLong() {
            super(Long.class);
        }

        @Override
        public Long convert(String obj) throws ConversionException {
            return Long.valueOf(obj);
        }

        @Override
        protected Long convert(Number number) throws ConversionException {
            return number.longValue();
        }
    }

    public static class StringToShort extends AbstractConverter<String, Short> {
        public StringToShort() {
            super(String.class, Short.class);
        }

        @Override
        public Short convert(String obj) throws ConversionException {
            return Short.valueOf(obj);
        }
    }

    @Override
    public void loadConverters() {
        Converters.loadContainedConverters(NumberConverters.class);

        Converters.registerConverter(new GenericNumberToDouble<>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToDouble<>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToDouble<>(Byte.class));
        Converters.registerConverter(new GenericNumberToDouble<>(Float.class));
        Converters.registerConverter(new GenericNumberToDouble<>(Integer.class));
        Converters.registerConverter(new GenericNumberToDouble<>(Long.class));
        Converters.registerConverter(new GenericNumberToDouble<>(Short.class));

        Converters.registerConverter(new GenericNumberToFloat<>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToFloat<>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToFloat<>(Byte.class));
        Converters.registerConverter(new GenericNumberToFloat<>(Double.class));
        Converters.registerConverter(new GenericNumberToFloat<>(Integer.class));
        Converters.registerConverter(new GenericNumberToFloat<>(Long.class));
        Converters.registerConverter(new GenericNumberToFloat<>(Short.class));

        Converters.registerConverter(new GenericNumberToInteger<>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToInteger<>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToInteger<>(Byte.class));
        Converters.registerConverter(new GenericNumberToInteger<>(Double.class));
        Converters.registerConverter(new GenericNumberToInteger<>(Float.class));
        Converters.registerConverter(new GenericNumberToInteger<>(Long.class));
        Converters.registerConverter(new GenericNumberToInteger<>(Short.class));

        Converters.registerConverter(new GenericSingletonToList<>(BigDecimal.class));
        Converters.registerConverter(new GenericSingletonToList<>(BigInteger.class));
        Converters.registerConverter(new GenericSingletonToList<>(Byte.class));
        Converters.registerConverter(new GenericSingletonToList<>(Double.class));
        Converters.registerConverter(new GenericSingletonToList<>(Float.class));
        Converters.registerConverter(new GenericSingletonToList<>(Integer.class));
        Converters.registerConverter(new GenericSingletonToList<>(Long.class));
        Converters.registerConverter(new GenericSingletonToList<>(Short.class));

        Converters.registerConverter(new GenericNumberToLong<>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToLong<>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToLong<>(Byte.class));
        Converters.registerConverter(new GenericNumberToLong<>(Double.class));
        Converters.registerConverter(new GenericNumberToLong<>(Float.class));
        Converters.registerConverter(new GenericNumberToLong<>(Integer.class));
        Converters.registerConverter(new GenericNumberToLong<>(Short.class));

        Converters.registerConverter(new GenericSingletonToSet<>(BigDecimal.class));
        Converters.registerConverter(new GenericSingletonToSet<>(BigInteger.class));
        Converters.registerConverter(new GenericSingletonToSet<>(Byte.class));
        Converters.registerConverter(new GenericSingletonToSet<>(Double.class));
        Converters.registerConverter(new GenericSingletonToSet<>(Float.class));
        Converters.registerConverter(new GenericSingletonToSet<>(Integer.class));
        Converters.registerConverter(new GenericSingletonToSet<>(Long.class));
        Converters.registerConverter(new GenericSingletonToSet<>(Short.class));

        Converters.registerConverter(new GenericNumberToShort<>(Integer.class));
        Converters.registerConverter(new GenericNumberToShort<>(Long.class));
    }
}
