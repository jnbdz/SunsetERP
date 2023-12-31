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
import org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;

import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Miscellaneous Converter classes. */
public class MiscConverters implements ConverterLoader {

    private static final int CHAR_BUFFER_SIZE = 4096;

    public static class BlobToBlob extends AbstractConverter<Blob, Blob> {
        public BlobToBlob() {
            super(Blob.class, Blob.class);
        }

        @Override
        public Blob convert(Blob obj) throws ConversionException {
            try {
                return new javax.sql.rowset.serial.SerialBlob(obj.getBytes(1, (int) obj.length()));
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class BlobToByteArray extends AbstractConverter<Blob, byte[]> {
        public BlobToByteArray() {
            super(Blob.class, byte[].class);
        }

        @Override
        public byte[] convert(Blob obj) throws ConversionException {
            try {
                return obj.getBytes(1, (int) obj.length());
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class ByteArrayToBlob extends AbstractConverter<byte[], Blob> {
        public ByteArrayToBlob() {
            super(byte[].class, Blob.class);
        }

        @Override
        public Blob convert(byte[] obj) throws ConversionException {
            try {
                return new javax.sql.rowset.serial.SerialBlob(obj);
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class ByteBufferToByteArray extends AbstractConverter<ByteBuffer, byte[]> {
        public ByteBufferToByteArray() {
            super(ByteBuffer.class, byte[].class);
        }

        @Override
        public byte[] convert(ByteBuffer obj) throws ConversionException {
            try {
                return obj.hasArray() ? obj.array() : null;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class ByteArrayToByteBuffer extends AbstractConverter<byte[], ByteBuffer> {
        public ByteArrayToByteBuffer() {
            super(byte[].class, ByteBuffer.class);
        }

        @Override
        public ByteBuffer convert(byte[] obj) throws ConversionException {
            try {
                return ByteBuffer.wrap(obj);
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class ClobToString extends AbstractConverter<Clob, String> {
        public ClobToString() {
            super(Clob.class, String.class);
        }

        @Override
        public String convert(Clob obj) throws ConversionException {
            StringBuilder strBuf = new StringBuilder();
            char[] inCharBuffer = new char[CHAR_BUFFER_SIZE];
            int charsRead = 0;
            try (Reader clobReader = obj.getCharacterStream()) {
                while ((charsRead = clobReader.read(inCharBuffer, 0, CHAR_BUFFER_SIZE)) > 0) {
                    strBuf.append(inCharBuffer, 0, charsRead);
                }
            } catch (Exception e) {
                throw new ConversionException(e);
            }
            return strBuf.toString();
        }
    }

    public static class EnumToString extends AbstractConverter<Enum<?>, String> {
        public EnumToString() {
            super(Enum.class, String.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return Enum.class.isAssignableFrom(sourceClass) && String.class.isAssignableFrom(targetClass);
        }

        @Override
        public String convert(Enum<?> obj) throws ConversionException {
            return obj.name();
        }

        @Override
        public String convert(Class<? extends String> targetClass, Enum<?> obj) throws ConversionException {
            return convert(obj);
        }

        @Override
        public Class<? super Enum<?>> getSourceClass() {
            return null;
        }
    }

    public static class StringToEnumConverterCreator<E extends Enum<E>> implements ConverterCreator, ConverterLoader {
        @Override
        public void loadConverters() {
            Converters.registerCreator(this);
        }

        @Override
        public <S, T> Converter<S, T> createConverter(Class<S> sourceClass, Class<T> targetClass) {
            if (String.class == sourceClass && Enum.class.isAssignableFrom(targetClass)) {
                return UtilGenerics.cast(new StringToEnum<>());
            }
            return null;
        }
    }

    private static class StringToEnum<E extends Enum<E>> extends AbstractConverter<String, E> {
        StringToEnum() {
            super(String.class, Enum.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return String.class.isAssignableFrom(sourceClass) && Enum.class.isAssignableFrom(targetClass);
        }

        @Override
        public E convert(String obj) throws ConversionException {
            throw new UnsupportedOperationException();
        }

        @Override
        public E convert(Class<? extends E> targetClass, String obj) throws ConversionException {
            return Enum.valueOf(UtilGenerics.<Class<E>>cast(targetClass), obj);
        }

        @Override
        public Class<? super Enum<?>> getTargetClass() {
            return null;
        }
    }

    public static class LocaleToString extends AbstractConverter<Locale, String> {
        public LocaleToString() {
            super(Locale.class, String.class);
        }

        @Override
        public String convert(Locale obj) throws ConversionException {
            return obj.toString();
        }
    }

    public static class StringToClob extends AbstractConverter<String, Clob> {
        public StringToClob() {
            super(String.class, Clob.class);
        }

        @Override
        public Clob convert(String obj) throws ConversionException {
            try {
                return new javax.sql.rowset.serial.SerialClob(obj.toCharArray());
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToLocale extends AbstractConverter<String, Locale> {
        public StringToLocale() {
            super(String.class, Locale.class);
        }

        @Override
        public Locale convert(String obj) throws ConversionException {
            Locale loc = UtilMisc.parseLocale(obj);
            if (loc != null) {
                return loc;
            }
            throw new ConversionException("Could not convert " + obj + " to Locale: ");
        }
    }

    public static class DecimalFormatToString extends AbstractConverter<DecimalFormat, String> {
        public DecimalFormatToString() {
            super(DecimalFormat.class, String.class);
        }

        @Override
        public String convert(DecimalFormat obj) throws ConversionException {
            return obj.toPattern();
        }
    }

    public static class StringToDecimalFormat extends AbstractConverter<String, DecimalFormat> {
        public StringToDecimalFormat() {
            super(String.class, DecimalFormat.class);
        }

        @Override
        public DecimalFormat convert(String obj) throws ConversionException {
            return new DecimalFormat(obj);
        }
    }

    public static class SimpleDateFormatToString extends AbstractConverter<SimpleDateFormat, String> {
        public SimpleDateFormatToString() {
            super(SimpleDateFormat.class, String.class);
        }

        @Override
        public String convert(SimpleDateFormat obj) throws ConversionException {
            return obj.toPattern();
        }
    }

    public static class StringToSimpleDateFormat extends AbstractConverter<String, SimpleDateFormat> {
        public StringToSimpleDateFormat() {
            super(String.class, SimpleDateFormat.class);
        }

        @Override
        public SimpleDateFormat convert(String obj) throws ConversionException {
            return new SimpleDateFormat(obj);
        }
    }

    public static class CharsetToString extends AbstractConverter<Charset, String> {
        public CharsetToString() {
            super(Charset.class, String.class);
        }

        @Override
        public String convert(Charset obj) throws ConversionException {
            return obj.name();
        }
    }

    public static class StringToCharset extends AbstractConverter<String, Charset> {
        public StringToCharset() {
            super(String.class, Charset.class);
        }

        @Override
        public Charset convert(String obj) throws ConversionException {
            return Charset.forName(obj);
        }
    }

    public static class StringBufferToString extends AbstractConverter<StringBuffer, String> {
        public StringBufferToString() {
            super(StringBuffer.class, String.class);
        }

        @Override
        public String convert(StringBuffer obj) throws ConversionException {
            return obj.toString();
        }
    }

    public static class StringWrapperToString extends AbstractConverter<StringUtil.StringWrapper, String> {
        public StringWrapperToString() {
            super(StringUtil.StringWrapper.class, String.class);
        }

        @Override
        public String convert(StringUtil.StringWrapper obj) {
            return obj.toString();
        }
    }

    public static class UUIDToString extends AbstractConverter<UUID, String> {
        public UUIDToString() {
            super(UUID.class, String.class);
        }

        @Override
        public String convert(UUID obj) throws ConversionException {
            return obj.toString();
        }
    }

    public static class StringToUUID extends AbstractConverter<String, UUID> {
        public StringToUUID() {
            super(String.class, UUID.class);
        }

        @Override
        public UUID convert(String obj) throws ConversionException {
            return UUID.fromString(obj);
        }
    }

    public static class RegexPatternToString extends AbstractConverter<Pattern, String> {
        public RegexPatternToString() {
            super(Pattern.class, String.class);
        }

        @Override
        public String convert(Pattern obj) throws ConversionException {
            return obj.toString();
        }
    }

    public static class StringToRegexPattern extends AbstractConverter<String, Pattern> {
        public StringToRegexPattern() {
            super(String.class, Pattern.class);
        }

        @Override
        public Pattern convert(String obj) throws ConversionException {
            return Pattern.compile(obj);
        }
    }

    public static class NotAConverterHelper {
        protected NotAConverterHelper() {
            throw new Error("Should not be loaded");
        }
    }

    public static class NotAConverter {
        public NotAConverter() {
        }
    }

    @Override
    public void loadConverters() {
        Converters.loadContainedConverters(MiscConverters.class);
    }
}
