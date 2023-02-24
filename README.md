# mnbt
[![](https://jitpack.io/v/Cmyna/mnbt.svg)](https://jitpack.io/#Cmyna/mnbt)

An nbt serialization/deserialization JVM library 

A better nbt data processing tools on JVM, 
support all Nbt Tag format [specification](https://minecraft.fandom.com/wiki/NBT_format).
And also provide extension support any future Tag format (see [extension](#extends-more-type-of-nbt-tags-format))

### add dependencies
#### Gradle
First add JitPack Repository
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```
Then add mnbt dependencies
```groovy
dependencies {
    // api dependency
    implementation 'com.github.Cmyna.mnbt:api:alpha-1.0'
    // annotation dependency
    implementation 'com.github.Cmyna.mnbt:annotation:alpha-1.0'
}
```

### Example: use Mnbt to convert single primitive value/primitive array
convert a String to StringTag(kotlin):
```
import net.myna.mnbt.Mnbt;

val mnbt = Mnbt()
val str = "a string"
val tag = mnbt.toTag("tagName", str) // convert to a Tag object
val binaryNbtData = mnbt.toBytes("tagName", str) // convert to binary data
```

### Example: Use Mnbt to convert any Java/Kotlin class object to Nbt format

Java:
```
import net.myna.mnbt.Mnbt;
import net.myna.mnbt.reflect.MTypeToken;

class SamplePojo {
    private int member1;
    private String member2;
    public SamplePojo(int m1, String m2) [
        this.member1 = m1;
        this.member2 = m2;
    }
}

Mnbt mnbt = new Mnbt();
String tagName = "sample pojo";
SamplePojo pojo = new SamplePojo(5, "some string value");
// create binary nbt data
byte[] bytes = mnbt.toBytes(tagName, pojo, new MTypeToken<SamplePojo>() {});
// deserialize binary nbt data
SamplePojo pojo = mnbt.fromBytes<SamplePojo>(bytes, 0, new MTypeToken<SamplePojo>() {});
```

Kotlin:
```
import net.myna.mnbt.Mnbt
import net.myna.mnbt.reflect.MTypeToken

data class SampleDataClass(val i:Int, val str:String)

val mnbt = Mnbt()
val tagName = "sample kotlin data class"
val dataClassInst = SampleDataClass(0, "some string")
// create binary nbt data
val bytes = mnbt.toBytes(tagName, dataClassInst, object:MTypeToken<SampleDataClas>() {})
// deserialize binary nbt data
val dataClassInst2 = mnbt.fromBytes(bytes, 0, object:MTypeToken<SampleDataClas>() {})
```

### Extends More type of Nbt Tags format

to extends a new Tag type (for example: a DateTag with tag id -1), 
just implement Tag abstract class like below:
```
class DateTag(override val name:String?, override val value:Date):Tag<Date>() {
    override val id = -1
    override fun valueToString():String {
        return value.toString()
    }
}
```
Also, to handle this new Tag type, should also implement 
`Codec` and `TagConverter` class

### Example: use Annotation

Mnbt support using annotation to remap the classes/fields to a specific 
Nbt structure, take map Level.dat nbt data as example:
```kotlin
@LocateAt("./Data/Version") // map to a relate Nbt tag structure
data class LevelVersion(
    @LocateAt("Id")
    val id:Int, // remap to tag with name "Id"
    @LocateAt("Name")
    val name:String, // remap to tag with name "Name"
    val Snapshot: Byte, // no mapping, the tag name is "Snapshot"
)
```
we know the Level.dat nbt structure is: an unnamed root tag contains a
sub-Compound tag called "Data", where there is a Compound Tag called "Version". 
So the class with `LocateAt` annotation shows this structure. Then 
we can easily get correct `LevelVersion` object with correct data
when there is already an `Tag` object represent Level.dat nbt structure:
```kotlin
val levelTag:CompoundTag = getLevelTag()
val levelVersion = Mnbt().fromTag(levelTag, object:MTypeToken<LevelVersion>() {})
```

There are other Annotations such as `Ignore`/`IgnoreWhenEncode`/`IgnoreWhenDecode`,
which can declare a field will
be ignored in converting to a Tag or de-converting from a Tag
