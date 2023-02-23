package net.myna.mnbt;

import net.myna.mnbt.reflect.MTypeToken;
import net.myna.mnbt.tag.CompoundTag;
import net.myna.mnbt.tag.DoubleTag;
import net.myna.mnbt.tag.ListTag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//TODO: complete most of the api used in java
// use encode/decode, use toTag/fromTag
public class ConverterJTest {

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

    @Test
    public void testIterable() {
        double[] doubles = {0.0,-819.5,23335.23,987654};
        List<Double> doubleList = new ArrayList<>();
        for (double d : doubles) {
            doubleList.add(d);
        }
        Mnbt mnbt = new Mnbt();
        Tag<?> tag = mnbt.toTag("doublesTag", doubleList);
        assert tag instanceof ListTag;
        assertEquals(doubles.length, ((ListTag<DoubleTag>)tag).getValue().size());
        for (int i=0; i<doubles.length; i++) {
            assertEquals(doubles[i], ((ListTag<DoubleTag>)tag).getValue().get(i).getValue());
        }
        List<Double> list2 = mnbt.fromTag(tag, new MTypeToken<List<Double>>() {}).getSecond();
        assertEquals(doubleList.size(), list2.size());
    }

}
