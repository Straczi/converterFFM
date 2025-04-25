package de.strocz;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

public class MemoryStruct<T> {
    private StructLayout structLayout;
    private MemorySegment segment;
    private int maxStringLength;
    private Class<T> clazz;

    public MemoryStruct(Arena arena, T obj) throws IllegalArgumentException, IllegalAccessException {
        this(arena, obj, 255);
    }

    @SuppressWarnings("unchecked")
    public MemoryStruct(Arena arena, T obj, int maxStringLength)
            throws IllegalArgumentException, IllegalAccessException {
        this.maxStringLength = maxStringLength;
        this.clazz = (Class<T>) obj.getClass();
        this.structLayout = getMemoryLayout();
        copyToMemory(obj, arena);
    }

    private StructLayout getMemoryLayout() throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = this.clazz.getDeclaredFields();
        MemoryLayout[] memoryLayouts = new MemoryLayout[fields.length];
        int i = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            switch (field.getType().getName()) {
                case "int" -> memoryLayouts[i] = ValueLayout.JAVA_INT.withName(field.getName());
                case "java.lang.String" -> memoryLayouts[i] = ValueLayout.ADDRESS.withName(field.getName());
                case "double" -> memoryLayouts[i] = ValueLayout.JAVA_DOUBLE.withName(field.getName());
                case "long" -> memoryLayouts[i] = ValueLayout.JAVA_LONG.withName(field.getName());
                case "float" -> memoryLayouts[i] = ValueLayout.JAVA_FLOAT.withName(field.getName());
                case "char" -> memoryLayouts[i] = ValueLayout.JAVA_CHAR.withName(field.getName());
                case "byte" -> memoryLayouts[i] = ValueLayout.JAVA_BYTE.withName(field.getName());
                case "short" -> memoryLayouts[i] = ValueLayout.JAVA_SHORT.withName(field.getName());
                case "boolean" -> memoryLayouts[i] = ValueLayout.JAVA_BOOLEAN.withName(field.getName());
                default -> throw new IllegalArgumentException("Unsupported field type: " + field.getType());
            }
            i++;
        }
        return MemoryLayout.structLayout(memoryLayouts);
    }

    private void copyToMemory(T obj, Arena arena) throws IllegalArgumentException, IllegalAccessException {
        this.segment = arena.allocate(structLayout);
        for (Field field : this.clazz.getDeclaredFields()) {
            field.setAccessible(true);
            VarHandle vh = this.structLayout.varHandle(PathElement.groupElement(field.getName()));
            if (field.getType() == String.class) {
                MemorySegment ms = arena.allocateFrom((String) field.get(obj));
                vh.set(segment, 0, ms);
            } else {
                vh.set(segment, 0, field.get(obj));
            }
        }
    }

    private String readNullTerminatedString(MemorySegment segment) {
        return segment.reinterpret(this.maxStringLength).getString(0);
    }

    public Object getField(String fieldName) {
        VarHandle vh = this.structLayout.varHandle(PathElement.groupElement(fieldName));
        Object value = vh.get(this.segment, 0);
        if (value instanceof MemorySegment) {
            return readNullTerminatedString((MemorySegment) value);
        }
        return vh.get(this.segment, 0);
    }

    public T convertBackToEntity() {
        try {
            T newObj = this.clazz.getDeclaredConstructor().newInstance();
            for (Field field : this.clazz.getDeclaredFields()) {
                field.setAccessible(true);
                field.set(newObj, getField(field.getName()));
            }
            return newObj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
