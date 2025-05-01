package de.strocz;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MemoryStruct<T> {
    private StructLayout structLayout;
    private MemorySegment segment;
    private int maxStringLength;
    private Class<T> clazz;
    private static final int STRUCT_LAYOUT_BYTE_SIZE = 8;

    public MemoryStruct(Arena arena, T obj) throws IllegalArgumentException, IllegalAccessException {
        this(arena, obj, 255);
    }

    @SuppressWarnings("unchecked")
    public MemoryStruct(Arena arena, T obj, int maxStringLength)
            throws IllegalArgumentException, IllegalAccessException {
        this.maxStringLength = maxStringLength;
        this.clazz = (Class<T>) obj.getClass();
        this.structLayout = getMemoryLayout(obj);
        copyToMemory(obj, arena);
    }

    private StructLayout getMemoryLayout(T obj) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = this.clazz.getDeclaredFields();
        List<MemoryLayout> memoryLayouts = new LinkedList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            ValueLayout valueLayout = getValueLayoutForClass(field.getType(), field.getName());
            memoryLayouts.add(valueLayout);
            if (valueLayout.byteAlignment() < STRUCT_LAYOUT_BYTE_SIZE) {
                memoryLayouts.add(MemoryLayout.paddingLayout(8 - valueLayout.byteAlignment()));
            }
        }
        return MemoryLayout.structLayout(memoryLayouts.toArray(new MemoryLayout[memoryLayouts.size()]));
    }

    private ValueLayout getValueLayoutForClass(Class<?> attributeClass, String name) throws IllegalArgumentException {
        // Collection case
        if (Collection.class.isAssignableFrom(attributeClass)) {
            return ValueLayout.ADDRESS.withName(name);
        }
        // other viable cases
        ValueLayout valueLayout;
        switch (attributeClass.getName()) {
            case "java.lang.Integer":
            case "int":
                valueLayout = ValueLayout.JAVA_INT.withName(name);
                break;
            case "java.lang.String":
                valueLayout = ValueLayout.ADDRESS.withName(name);
                break;
            case "java.lang.Double":
            case "double":
                valueLayout = ValueLayout.JAVA_DOUBLE.withName(name);
                break;
            case "java.lang.Long":
            case "long":
                valueLayout = ValueLayout.JAVA_LONG.withName(name);
                break;
            case "java.lang.Float":
            case "float":
                valueLayout = ValueLayout.JAVA_FLOAT.withName(name);
                break;
            case "java.lang.Char":
            case "char":
                valueLayout = ValueLayout.JAVA_CHAR.withName(name);
                break;
            case "java.lang.Byte":
            case "byte":
                valueLayout = ValueLayout.JAVA_BYTE.withName(name);
                break;
            case "java.lang.Short":
            case "short":
                valueLayout = ValueLayout.JAVA_SHORT.withName(name);
                break;
            case "java.lang.Boolean":
            case "boolean":
                valueLayout = ValueLayout.JAVA_BOOLEAN.withName(name);
                break;
            default:
                throw new IllegalArgumentException("Unsupported field type: " + attributeClass);
        }
        return valueLayout;
    }

    private void copyToMemory(T obj, Arena arena) throws IllegalArgumentException, IllegalAccessException {
        this.segment = arena.allocate(structLayout);
        for (Field field : this.clazz.getDeclaredFields()) {
            field.setAccessible(true);
            VarHandle vh = this.structLayout.varHandle(PathElement.groupElement(field.getName()));
            if (field.getType() == String.class) {
                MemorySegment ms = arena.allocateFrom((String) field.get(obj));
                vh.set(segment, 0, ms);
            } else if (Collection.class.isAssignableFrom(field.getType())) {
                MemorySegment ms = getMemorySegmentForCollection((Collection<?>) field.get(obj), arena);
                vh.set(segment, 0, ms);
            } else {
                vh.set(segment, 0, field.get(obj));
            }
        }
    }

    private MemorySegment getMemorySegmentForCollection(Collection<?> collection, Arena arena) {
        if (collection.isEmpty()) {
            return null;
        }
        if (collection instanceof List<?>) {
            return getMemorySegmentForListOrSet(collection, arena);
        } else {
            throw new UnsupportedOperationException("Collection type not supported: " + collection.getClass());
        }
    }

    private MemorySegment getMemorySegmentForListOrSet(Collection<?> collection, Arena arena) {
        long size = collection.size();
        Iterator<?> it = collection.iterator();
        Object currentElement = it.next();
        SequenceLayout layout = MemoryLayout.sequenceLayout(size,
                MemoryLayout.structLayout(getValueLayoutForClass(currentElement.getClass(), "collElement")));

        // set size of collection
        MemoryLayout collectionLayout = MemoryLayout.structLayout(ValueLayout.JAVA_LONG.withName("size"),
                layout.withName("seq"));
        VarHandle sizeHandle = collectionLayout.varHandle(PathElement.groupElement("size"));
        
        //get varHandle for elements
        VarHandle vh = layout.varHandle(PathElement.sequenceElement(), PathElement.groupElement("collElement"));
        MemorySegment segment = arena.allocate(collectionLayout);
        sizeHandle.set(segment, 0, size);

        boolean isString = currentElement instanceof String;
        long offset = 0;
        while (currentElement != null) {
            if (isString) {
                MemorySegment ms = arena.allocateFrom((String) currentElement);
                vh.set(segment, 8l, offset, ms);
            } else {
                vh.set(segment, 8l, offset, currentElement);
            }
            offset++;
            currentElement = it.hasNext() ? it.next() : null;
        }
        return segment;
    }

    private String readNullTerminatedString(MemorySegment segment) {
        return segment.reinterpret(this.maxStringLength).getString(0);
    }

    public Object getFieldValue(String fieldName) throws NoSuchFieldException {
        VarHandle vh = this.structLayout.varHandle(PathElement.groupElement(fieldName));
        Object value = vh.get(this.segment, 0);
        Field field = this.clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        if (field.getType() == String.class) {
            return readNullTerminatedString((MemorySegment) value);
        } else if (List.class.isAssignableFrom(field.getType())) {
            String typename = field.getGenericType().getTypeName();
            String genericName = typename.substring(typename.indexOf('<') + 1, typename.lastIndexOf('>'));
            return getFieldValueForList((MemorySegment) value, genericName);
        } else if(Collection.class.isAssignableFrom(field.getType())){
            throw new IllegalArgumentException("Unsupported field type: " + field.getType());
        }
        return vh.get(this.segment, 0);
    }


    private Object getFieldValueForList(MemorySegment value, String type) {
        long size = value.reinterpret(8l).get(ValueLayout.JAVA_LONG, 0);
        switch (type) {
            case "java.lang.Integer": {
                List<Integer> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    int iValue = value.reinterpret(8l + 4 * size).get(ValueLayout.JAVA_INT, 4l * i + 8l);
                    list.add(iValue);
                }
                return list;
            }
            case "java.lang.String": {
                List<String> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    MemorySegment ms = value.reinterpret(8l + 8 * size).get(ValueLayout.ADDRESS, 8l * i + 8l);
                    String s = readNullTerminatedString(ms);
                    list.add(s);
                }
                return list;
            }
            case "java.lang.Double": {
                List<Double> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    double dValue = value.reinterpret(8l + 8 * size).get(ValueLayout.JAVA_DOUBLE, 8l * i + 8l);
                    list.add(dValue);
                }
                return list;
            }
            case "java.lang.Long": {
                List<Long> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    long lValue = value.reinterpret(8l + 8 * size).get(ValueLayout.JAVA_LONG, 8l * i + 8l);
                    list.add(lValue);
                }
                return list;
            }
            case "java.lang.Float": {
                List<Float> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    float fValue = value.reinterpret(8l + 4 * size).get(ValueLayout.JAVA_FLOAT, 4l * i + 8l);
                    list.add(fValue);
                }
                return list;
            }
            case "java.lang.Char": {
                List<Character> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    char cValue = value.reinterpret(8l + 2 * size).get(ValueLayout.JAVA_CHAR, 2l * i + 8l);
                    list.add(cValue);
                }
                return list;
            }
            case "java.lang.Byte": {
                List<Byte> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    byte bValue = value.reinterpret(8l + 1 * size).get(ValueLayout.JAVA_BYTE, 1l * i + 8l);
                    list.add(bValue);
                }
                return list;
            }
            case "java.lang.Short": {
                List<Short> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    short sValue = value.reinterpret(8l + 2 * size).get(ValueLayout.JAVA_SHORT, 2l * i + 8l);
                    list.add(sValue);
                }
                return list;
            }
            case "java.lang.Boolean": {
                List<Boolean> list = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    boolean bValue = value.reinterpret(8l + 1 * size).get(ValueLayout.JAVA_BOOLEAN, 1l * i + 8l);
                    list.add(bValue);
                }
                return list;
            }
            default:
                throw new IllegalArgumentException("Unsupported field type: " + type);
        }

    }

    public T convertBackToEntity() {
        try {
            T newObj = this.clazz.getDeclaredConstructor().newInstance();
            for (Field field : this.clazz.getDeclaredFields()) {
                field.setAccessible(true);
                field.set(newObj, getFieldValue(field.getName()));
            }
            return newObj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MemorySegment getSegment() {
        return this.segment;
    }
}
