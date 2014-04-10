package io.github.furikuri.unsafe;

import sun.misc.Unsafe;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class UnsafeUtil {
    public static final int BYTE = 8;

    public static final int BOOLEAN_SIZE = 1;
    public static final int BYTE_SIZE = Byte.SIZE / BYTE;
    public static final int CHAR_SIZE = Character.SIZE / BYTE;
    public static final int SHORT_SIZE = Short.SIZE / BYTE;
    public static final int INT_SIZE = Integer.SIZE / BYTE;
    public static final int FLOAT_SIZE = Float.SIZE / BYTE;
    public static final int LONG_SIZE = Long.SIZE / BYTE;
    public static final int DOUBLE_SIZE = Double.SIZE / BYTE;

    private static Map<Class<?>, Integer> classSizes = new HashMap<Class<?>, Integer>();

    public static Unsafe unsafe = getUnsafe();

    {
        classSizes.put(boolean.class, BOOLEAN_SIZE);
        classSizes.put(byte.class, BYTE_SIZE);
        classSizes.put(char.class, CHAR_SIZE);
        classSizes.put(short.class, SHORT_SIZE);
        classSizes.put(int.class, INT_SIZE);
        classSizes.put(float.class, FLOAT_SIZE);
        classSizes.put(long.class, LONG_SIZE);
        classSizes.put(boolean.class, DOUBLE_SIZE);
    }

    public static long sizeForObject(Class<?> clazz) {
        long size = 12;
        for (;clazz != null; clazz = clazz.getSuperclass()) {
            final Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    size = adjustForField(size, f);
                }
            }
        }
        return alignObjectSize(size);
    }

    public static long alignObjectSize(long size) {
        size += (long) 8 - 1L;
        return size - (size % 8);
    }

    private static long adjustForField(long sizeSoFar, final Field f) {
        f.setAccessible(true);
        final Class<?> type = f.getType();
        final int fsize = sizeOfType(type);
        long offsetPlusSize = 0;
        if (Modifier.isStatic(f.getModifiers())) {
            offsetPlusSize = unsafe.staticFieldOffset(f) + fsize;
        }
        else {
            offsetPlusSize = unsafe.objectFieldOffset(f) + fsize;
        }
        return Math.max(sizeSoFar, offsetPlusSize);
    }

    public static long addressOf(Object obj) {
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);

        Object[] objArray = new Object[1];

        if (obj == null) {
            return 0;
        }
        objArray[0] = obj;
        return toNativeAddress(normalize(unsafe.getInt(objArray, baseOffset)));
    }

    public static long getAddressForClass(Class clazz) {
        return toNativeAddress(normalize(unsafe.getInt(addressOf(clazz) + 84)));
    }

    public static int sizeOfType(Class<?> type) {
        if (classSizes.containsKey(type)) {
            return classSizes.get(type);
        } else {
            return 4;
        }
    }

    public static long toNativeAddress(long address) {
        return address << 3;
    }

    public static long normalize(int value) {
        if (value >= 0) {
            return value;
        }
        else {
            return (~0L >>> 32) & value;
        }
    }

    public static void dump(PrintStream ps, long address, long size) {
        for (int i = 0; i < size; i++) {
            if (i % 16 == 0) {
                ps.print(String.format("[0x%04x]: ", i));
            }
            ps.print(String.format("%02x ", unsafe.getByte(address + i)));
            if ((i + 1) % 16 == 0) {
                ps.println();
            }
        }
        ps.println();
    }

    public static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
