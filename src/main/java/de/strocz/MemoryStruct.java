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
import java.util.Map;
import java.util.Set;

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
            if(valueLayout.byteAlignment() < STRUCT_LAYOUT_BYTE_SIZE){
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
        switch (attributeClass.getName()) { //TODO: this may have to be rewritten to also accept java.lang.Integer (and so on)
            case "int" -> valueLayout = ValueLayout.JAVA_INT.withName(name);
            case "java.lang.String" -> valueLayout = ValueLayout.ADDRESS.withName(name);
            case "double" -> valueLayout = ValueLayout.JAVA_DOUBLE.withName(name);
            case "long" -> valueLayout = ValueLayout.JAVA_LONG.withName(name);
            case "float" -> valueLayout = ValueLayout.JAVA_FLOAT.withName(name);
            case "char" -> valueLayout = ValueLayout.JAVA_CHAR.withName(name);
            case "byte" -> valueLayout = ValueLayout.JAVA_BYTE.withName(name);
            case "short" -> valueLayout = ValueLayout.JAVA_SHORT.withName(name);
            case "boolean" -> valueLayout = ValueLayout.JAVA_BOOLEAN.withName(name);
            default -> throw new IllegalArgumentException("Unsupported field type: " + attributeClass);
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
        if (collection instanceof List<?> || collection instanceof Set<?>) {
            return getMemorySegmentForListOrSet(collection, arena);
        } else if (collection instanceof Map<?, ?>) {
            return getMemorySegmentForMap((Map<?, ?>) collection, arena);
        }

        return null;
    }

    private MemorySegment getMemorySegmentForListOrSet(Collection<?> collection, Arena arena) {
        long size = collection.size();
        Iterator<?> it = collection.iterator();
        Object currentElement = it.next();
        SequenceLayout layout = MemoryLayout.sequenceLayout(size,
                MemoryLayout.structLayout(getValueLayoutForClass(currentElement.getClass(), "collElement")));
        VarHandle vh = layout.varHandle(PathElement.sequenceElement(), PathElement.groupElement("collElement"));

        //set size of collection
        MemoryLayout collectionLayout = MemoryLayout.structLayout(ValueLayout.JAVA_LONG.withName("size"), layout);
        MemorySegment segment = arena.allocate(collectionLayout);
        VarHandle sizeHandle = collectionLayout.varHandle(PathElement.groupElement("size"));
        sizeHandle.set(segment, 0, size);
        
        boolean isString = currentElement instanceof String;
        long offset = 0;
        while(currentElement != null){
            if(isString) {
                MemorySegment ms = arena.allocateFrom((String) currentElement);
                vh.set(segment, 8l ,offset, ms);
            } else {
                vh.set(segment, 8l, offset, currentElement);  //TODO: get this to work with other types besides String!
            }
            offset++;
            currentElement = it.hasNext() ? it.next() : null;
        }
        return segment;
    }

    private MemorySegment getMemorySegmentForMap(Map<?, ?> map, Arena arena) {
        throw new UnsupportedOperationException("Map is not supported yet");
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
        } else if(Collection.class.isAssignableFrom(field.getType())) {
            return getFieldValueForCollection((MemorySegment) value);
        }
        return vh.get(this.segment, 0);
    }

    //lets get funky wunky 
   private Object getFieldValueForCollection(MemorySegment value) {
        long size = value.reinterpret(8l).get(ValueLayout.JAVA_LONG, 0);
        List<String> list = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            MemorySegment ms = value.reinterpret(8l+ 8*size).get(ValueLayout.ADDRESS, 8l * (i + 1)); //TODO: get this to work with other types besides String!
            String s = readNullTerminatedString(ms);
            list.add(s);
        }
        return list;
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
