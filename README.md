# mnbt
An nbt serialization/deserialization library

### This Library is still in developing and everything is unstable!

Target: a better nbt data processing tools on JVM, which can be used like 
some json processing tools such as JackJson or Gson. For example: 

Now Mnbt can serialize POJO class to nbt binary data, or deserialize data to
a POJO class like code below:

Java:
```
import com.myna.mnbt.Mnbt;

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
import com.myna.mnbt.Mnbt

data class SampleDataClass(val i:Int, val str:String)
val mnbt = Mnbt()
val tagName = "sample kotlin data class"
val dataClassInst = SampleDataClass(0, "some string")
// create binary nbt data
val bytes = mnbt.toBytes(tagName, dataClassInst, object:MTypeToken<SampleDataClas>() {})
// deserialize binary nbt data
val dataClassInst2 = mnbt.fromBytes(bytes, 0, object:MTypeToken<SampleDataClas>() {})
```

