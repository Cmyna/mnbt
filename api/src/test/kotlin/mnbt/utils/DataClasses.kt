package mnbt.utils

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.annotations.FieldValueProvider
import com.myna.mnbt.annotations.Ignore
import com.myna.mnbt.annotations.LocateAt
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.ListTag
import java.lang.reflect.Field
import kotlin.random.Random
import kotlin.reflect.jvm.javaField

open class UseBeanObjEqFun {
    override fun equals(other: Any?):Boolean {
        return ApiTestTool.beanObjEqFun(this, other)
    }
}

data class DataClass1 (
    var i:Int,
    var j:String,
    var k:Long? = null // nullable member
): UseBeanObjEqFun()

fun newDataClass1(randomFieldValue: Boolean):DataClass1 {
    return DataClass1(
            if (randomFieldValue) Random.nextInt() else 777,
            if (randomFieldValue) RandomValueTool.bitStrC(10)() else "string field fix value for DataClass1",
            if (randomFieldValue) Random.nextLong() else -513201
    )
}



fun DataClass1.toCompound(name:String?): CompoundTag {
    val comp = CompoundTag(name)
    ApiTestValueBuildTool.prepareTag2("i", this.i).also { comp.add(it) }
    ApiTestValueBuildTool.prepareTag2("j", this.j).also { comp.add(it) }
    this.k?.apply { ApiTestValueBuildTool.prepareTag2("j", this).also { comp.add(it) } }
    return comp
}

data class DataClass2 (
    var dataClass1:DataClass1,
    var dc2D:Double
): UseBeanObjEqFun()

fun DataClass2.toCompound(name:String?): CompoundTag {
    return CompoundTag(name).also { root->
        this.dataClass1.toCompound("dataClass1").also {root.add(it)}
        ApiTestValueBuildTool.prepareTag2("dc2D", this.dc2D).also { root.add(it) }
    }
}

data class DataClass3 (
    var dataClass2List:List<DataClass2>,
    var dc3L:Long
): UseBeanObjEqFun()

fun DataClass3.toCompound(name:String?): CompoundTag {
    return CompoundTag(name).also { root ->
        ListTag<AnyCompound>(IdTagCompound, "dataClass2List").also { listTag->
            this.dataClass2List.onEach { dc2->
                listTag.add(dc2.toCompound(null))
            }
            root.add(listTag)
        }
        ApiTestValueBuildTool.prepareTag2("dc3L", this.dc3L).also { root.add(it) }
    }
}


/**
 * flat value data class with LocateAt Annotation
 */
@LocateAt("./midTag1/data class 4 entry/")
data class DataClass4(
        @LocateAt("./midTag2/int tag in data class 4 tag")
        val dc4Int:Int,
        @LocateAt("./string tag in data class 4 tag")
        val dc4String: String,
        /**
         * val without LocateAt Annotation
         */
        val longVar:Long,
        /**
         * with Ignore Annotation
         */
        @Ignore(true, true, DataClassesFieldsProvider::class)
        val dc1: DataClass1
): UseBeanObjEqFun()

fun newDataClass4(randomFieldValue:Boolean):DataClass4 {
    return DataClass4(
            if (randomFieldValue) Random.nextInt() else 515041,
            if (randomFieldValue) RandomValueTool.bitStrC(10)() else "data class 4 String field value",
            if (randomFieldValue) Random.nextLong() else 12345678910,
            newDataClass1(randomFieldValue)
    )
}

@LocateAt("./midTag3/data class 5 entry")
data class DataClass5(
        @LocateAt("./midTag4/data class 4 field entry")
        val dc4: DataClass4
)

/**
 *
 */
@LocateAt("./", "./#5")
data class DataClass6(
        val dc6Int:Int,
        val dc6Str:String,
        val dc6Float:Float
        )

fun newDataClass6(randomFieldValue: Boolean):DataClass6 {
    return DataClass6(
            if (randomFieldValue) Random.nextInt() else -10,
            if (randomFieldValue) RandomValueTool.bitStrC(15)() else "data class 6 string field value",
            if (randomFieldValue) Random.nextFloat() else 0.85f
    )
}

class DataClassesFieldsProvider:FieldValueProvider {
    override fun provide(field: Field): Any? {
        return when(field) {
            DataClass4::dc1.javaField -> newDataClass1(false)
            else -> null
        }
    }

}