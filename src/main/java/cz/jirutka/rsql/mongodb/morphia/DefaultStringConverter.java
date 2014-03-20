/*
 * The MIT License
 *
 * Copyright 2013-2014 Czech Technical University in Prague.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package cz.jirutka.rsql.mongodb.morphia;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Simple implementation of {@link StringConverter}
 * interface that can convert String to common Java types:
 * <tt>Boolean, Integer, Long, Double, Float, Date</tt>.
 *
 * <p>The date must be in one of these ISO 8601 formats:
 * <ul>
 *     <li>Datetime with time zone: <tt>yyyy-MM-dd'T'HH:mm:ssX</tt>,
 *         for example: 2014-03-20T15:30:42Z, 2014-03-20T15:30:42+01:00, ...</li>
 *     <li>Datetime in local zone: <tt>yyyy-MM-dd'T'HH:mm:ss</tt>,
 *         for example: 2014-03-20T15:30:42</li>
 *     <li>Date only: <tt>yyyy-MM-dd</tt>, for example: 2014-03-20</li>
 * </ul>
 * </p>
 *
 * <p>It can also convert to any class that implements method: <br />
 * <tt>public static T valueOf(String)</tt>, where <tt>T</tt> is the target
 * type.</p>
 *
 * <p>If you're using Spring Framework, then you should use
 * {@link SpringConversionServiceAdapter SpringConversionServiceAdapter}
 * with the Spring's {@link org.springframework.core.convert.ConversionService ConversionService}.
 * </p>
 */
@Immutable
public class DefaultStringConverter extends AbstractStringConverter {

    private static final Logger log = LoggerFactory.getLogger(DefaultStringConverter.class);

    private static final DateFormat[] ISO8601_DATE_FORMATS = new DateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"), // datetime with time zone (e.g. Z, +01:00, ...)
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),  // datetime in local zone
            new SimpleDateFormat("yyyy-MM-dd") };           // date only

    private static final Set<String>
            TRUE_VALUES = new HashSet<>(asList("true", "yes", "y")),
            FALSE_VALUES = new HashSet<>(asList("false", "no", "n"));


    @SuppressWarnings("unchecked")
    public <T> T convert(String value, Class<T> type) throws ArgumentFormatException {

        log.trace("Parsing argument '{}' as type: {}", value, type.getSimpleName());

        // common types
        try {
            if (type.equals(String.class)) {
                return (T) value;
            }
            if (type.equals(Integer.class)) {
                return (T) Integer.valueOf(value);
            }
            if (type.equals(Boolean.class)) {
                return (T) parseBoolean(value);
            }
            if (type.isEnum()) {
                return (T) parseEnum(value, (Class) type);
            }
            if (type.equals(Double.class)) {
                return (T) Double.valueOf(value);
            }
            if (type.equals(Float.class)) {
                return (T) Float.valueOf(value);
            }
            if (type.equals(Long.class)) {
                return (T) Long.valueOf(value);
            }
            if (type.equals(Date.class)) {
                return (T) parseDate(value);
            }
        } catch (IllegalArgumentException ex) {
            throw new ArgumentFormatException(value, type);
        }

        // try to parse via valueOf(String s) method
        try {
            log.trace("Trying to get and invoke valueOf(String s) method on {}", type);
            Method method = type.getMethod("valueOf", String.class);
            return (T) method.invoke(type, value);

        } catch (NoSuchMethodException ex) {
            log.debug("{} does not have method valueOf(String s)", type);

        } catch (InvocationTargetException ex) {
            throw new ArgumentFormatException(value, type);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        throw new ArgumentFormatException(value, type, "Cannot parse argument type: " + type);
    }


    protected Boolean parseBoolean(String value) {
        value = value.trim().toLowerCase();

        if (TRUE_VALUES.contains(value)) {
            return true;

        } else if (FALSE_VALUES.contains(value)) {
            return false;

        } else {
            throw new IllegalArgumentException("Invalid boolean value: " + value);
        }
    }

    protected <T extends Enum> T parseEnum(String value, Class<T> enumClass) {
        for (T e : enumClass.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(value.toUpperCase())) {
                return e;
            }
        }
        throw new ArgumentFormatException(value, enumClass);
    }

    protected Date parseDate(String value) {
        for (DateFormat formatter : ISO8601_DATE_FORMATS) {
            try {
                return formatter.parse(value);
            } catch (ParseException ex) {
                // ignore
            }
        }
        throw new ArgumentFormatException(value, Date.class);
    }
}
