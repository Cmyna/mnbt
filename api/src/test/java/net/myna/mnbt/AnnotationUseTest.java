package net.myna.mnbt;

import net.myna.mnbt.Mnbt;
import net.myna.mnbt.Tag;
import net.myna.mnbt.annotations.FieldValueProvider;
import net.myna.mnbt.annotations.Ignore;
import net.myna.mnbt.annotations.LocateAt;
import net.myna.mnbt.reflect.MTypeToken;
import net.myna.mnbt.tag.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


public class AnnotationUseTest {
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
