package net.myna.mnbt.annotation;

import net.myna.mnbt.annotations.LocateAt;
import org.junit.jupiter.api.Test;

public class UseTest {
    @Test
    public void testLocateAt() {

    }

    @LocateAt(toTagPath="./tag1/JClassATag")
    public class TestJClassA {
        private int i;
        private long l;
        @LocateAt(toTagPath="./tag2/strTag")
        public String str;
    }
}
