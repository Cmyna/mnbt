package net.myna.mnbt.annotation;

import net.myna.mnbt.Mnbt;
import net.myna.mnbt.Tag;
import net.myna.mnbt.annotations.FieldValueProvider;
import net.myna.mnbt.annotations.Ignore;
import net.myna.mnbt.annotations.LocateAt;
import net.myna.mnbt.converter.meta.NbtPathTool;
import net.myna.mnbt.reflect.MTypeToken;
import net.myna.mnbt.tag.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.myna.mnbt.ConstantsKt.*;
import static org.junit.jupiter.api.Assertions.*;

//TODO: complete most of the api used in java
// use encode/decode, use toTag/fromTag
public class UseTest {
    @Test
    public void testWithTestJClassA() {
        Mnbt mnbt = new Mnbt();
        TestJClassA obj1 = new TestJClassA();
        obj1.str = "some string";
        obj1.i = 233;
        obj1.l = 123456789;

        Tag<?> tag = mnbt.toTag("root", obj1);
        assert tag instanceof CompoundTag;
        assertEquals("root", tag.getName());
        // test LocateAt annotation
        assertNotNull(NbtPathTool.INSTANCE.findTag(tag, "./tag1/JClassATag"));
        assertNotNull(NbtPathTool.INSTANCE.findTag(tag, "./tag1/JClassATag/tag2/strTag"));

        // test Ignore annotation
        assertNull(NbtPathTool.INSTANCE.findTag(tag, "./tag1/JClassATag/bytes"));
        TestJClassA obj2 = Objects.requireNonNull(mnbt.fromTag(tag, new MTypeToken<TestJClassA>() {})).getSecond();
        assertEquals(233, obj2.i);
        assertEquals(123456789, obj2.l);
        assertEquals("some string", obj2.str);
        assertEquals(5, obj2.bytes.length);
        assertEquals(-127, obj2.bytes[4]);
    }

    @Test
    public void testMapConversion() {
        Map<String, Integer> intMap = new HashMap<>();
        intMap.put("int tag1", 55);
        intMap.put("intTag2", 0);
        intMap.put("intTag3", 9199);
        Mnbt mnbt = new Mnbt();
        Tag<?> tag = mnbt.toTag("int tag map", intMap);
        assertTrue(tag instanceof CompoundTag);
        assertEquals("int tag map", tag.getName());
        assertEquals(3, ((CompoundTag)tag).getValue().entrySet().size());
        assertEquals(55, ((CompoundTag)tag).getValue().get("int tag1").getValue());
        assertEquals(0, ((CompoundTag)tag).getValue().get("intTag2").getValue());
        assertEquals(9199, ((CompoundTag)tag).getValue().get("intTag3").getValue());

        Map<String, Integer> map2 = Objects.requireNonNull(mnbt.fromTag(tag, new MTypeToken<Map<String, Integer>>() {})).getSecond();
        assertEquals(3, map2.entrySet().size());
        assertEquals(55, map2.get("int tag1"));
        assertEquals(0, map2.get("intTag2"));
        assertEquals(9199, map2.get("intTag3"));
    }


    @LocateAt(toTagPath="./tag1/JClassATag")
    public static class TestJClassA {
        private int i;
        private long l;
        @LocateAt(toTagPath="./tag2/strTag")
        public String str;
        @Ignore(ignoreFromTag = true, ignoreToTag = true, fieldValueProvider = TestFieldProvider.class)
        protected byte[] bytes;
    }

    public static class TestFieldProvider implements FieldValueProvider {
        @Nullable
        @Override
        public Object provide(@NotNull Field field) {
            if (field.getDeclaringClass()==TestJClassA.class && field.getName().equals("bytes")) {
                return new byte[]{5,3,9,0,-127};
            }
            return null;
        }
    }
}
