package mnbt.utils

import com.myna.mnbt.IdTagCompound
import com.myna.mnbt.tag.AnyCompound
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.ListTag

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