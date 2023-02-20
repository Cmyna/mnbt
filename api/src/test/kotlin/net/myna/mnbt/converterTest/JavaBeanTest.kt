package net.myna.mnbt.converterTest

import net.myna.mnbt.*
import net.myna.mnbt.reflect.MTypeToken
import net.myna.mnbt.tag.AnyCompound
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.tag.ListTag
import net.myna.mnbt.utils.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class JavaBeanTest {

    private val converterTemplate = ApiTestTool.ConverterTestTemplate()


    /**
     * test java bean class (use ReflectiveConverter)
     */
    @Test
    fun flatBeanClassConversionTest() {
        val tk = object: MTypeToken<JavaBean>() {}
        val a = randomJavaBean()
        val a2 = randomJavaBean()

        val sameCompound = CompoundTag("A1").also{
            it.add(ApiTestValueBuildTool.prepareTag(a.i!!, "i"))
            it.add(ApiTestValueBuildTool.prepareTag(value = a.j!!, "j"))
            it.add(ApiTestValueBuildTool.prepareTag(a.k!!, "k"))
            it.add(ApiTestValueBuildTool.prepareTag(a.s!!, "s"))
            it.add(ApiTestValueBuildTool.prepareTag(a.l!!, "l"))
            it.add(ApiTestValueBuildTool.prepareTag(a.f!!, "f"))
            it.add(ApiTestValueBuildTool.prepareTag(a.d!!, "d"))
            it.add(ApiTestValueBuildTool.prepareTag(a.bits!!, "bits"))
            it.add(ApiTestValueBuildTool.prepareTag(a.ints!!, "ints"))
            it.add(ApiTestValueBuildTool.prepareTag(a.longs!!, "longs"))
            it.add(ApiTestValueBuildTool.prepareTag(a.b!!, "b"))
        }
        val template = ApiTestTool.ConverterTestTemplate()
        template.expectedTag = sameCompound
        template.apiTest(TestMnbt.inst.refConverterProxy, "A1", "A2", a, a2, tk)
    }

    @Test
    fun conversionTestForJavabeanContainsListContainsFlatValue() {
        val creation:(num:Int)-> JavaBeanWithFlatValueList = { num->
            JavaBeanWithFlatValueList(
                    ApiTestValueBuildTool.listPreparation(num) { Random.nextBytes(1)[0]},
                    ApiTestValueBuildTool.listPreparation(num) { Random.nextInt().toShort()},
                    ApiTestValueBuildTool.listPreparation(num) { Random.nextInt()},
                    ApiTestValueBuildTool.listPreparation(num) { Random.nextLong()},
                    ApiTestValueBuildTool.listPreparation(num) { Random.nextFloat()},
                    ApiTestValueBuildTool.listPreparation(num) { Random.nextDouble()},
                    ApiTestValueBuildTool.listPreparation(num) { RandomValueTool.bitStrC(5)()},
                    ApiTestValueBuildTool.listPreparation(num) { Random.nextBytes(200)},
                    ApiTestValueBuildTool.listPreparation(num) { RandomValueTool.intArrC(200)()},
                    ApiTestValueBuildTool.listPreparation(num) { RandomValueTool.longArrC(200)()}
            )
        }
        val obj1 = creation(10)
        val obj2 = creation(15)
        val name1 = "java bean with list1"
        val name2 = "java bean with list2"
        val compound = CompoundTag(name1).also { comp->
            ApiTestValueBuildTool.iterableToListTag("byteList", IdTagByte, obj1.byteList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("shortList", IdTagShort, obj1.shortList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("intList", IdTagInt, obj1.intList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("longList", IdTagLong, obj1.longList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("floatList", IdTagFloat, obj1.floatList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("doubleList", IdTagDouble, obj1.doubleList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("stringList", IdTagString, obj1.stringList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("bitsList", IdTagByteArray, obj1.bitsList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("intsList", IdTagIntArray, obj1.intsList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
            ApiTestValueBuildTool.iterableToListTag("longsList", IdTagLongArray, obj1.longsList!!, ApiTestValueBuildTool::prepareTag).also { comp.add(it) }
        }
        val typeToken = object: MTypeToken<JavaBeanWithFlatValueList>() {}
        converterTemplate.expectedTag = compound
        converterTemplate.apiTest(TestMnbt.inst.refConverterProxy, name1, name2, obj1, obj2, typeToken)
    }

    @Test
    fun conversionTestForBeanContainsIterableProperties() {
        val bean1 = JavaBeanWithFlatValueIterable()
        val bean2 = JavaBeanWithFlatValueIterable()
        val name1 = "bean1"
        val name2 = "bean2"
        val tk = object: MTypeToken<JavaBeanWithFlatValueIterable>() {}
        val emptyComp = CompoundTag(name1)
        // set reflective tag converter nullable properties
        TestMnbt.inst.refReflectiveConverter.returnObjectWithNullableProperties = true
        converterTemplate.expectedTag = emptyComp
        converterTemplate.assertNameNotEquals = false
        converterTemplate.assertValueNotEquals = false
        converterTemplate.apiTest(TestMnbt.inst.refConverterProxy, name1, name2, bean1, bean2, tk)


        bean1.byteArray = ApiTestValueBuildTool.listPreparation(300) { Random.nextBytes(1)[0] }.toTypedArray()
        bean1.strIterable = ApiTestValueBuildTool.listPreparation(27) { RandomValueTool.bitStrC(17)() }
        bean1.longArray = ApiTestValueBuildTool.listPreparation(0) { Random.nextLong() }.toTypedArray()
        bean2.byteArray = ApiTestValueBuildTool.listPreparation(300) { Random.nextBytes(1)[0] }.toTypedArray()
        bean2.strIterable = ApiTestValueBuildTool.listPreparation(55) { RandomValueTool.bitStrC(17)() }
        bean2.longArray = ApiTestValueBuildTool.listPreparation(200) { Random.nextLong() }.toTypedArray()

        val comp2 = CompoundTag(name1).also { comp->
            ApiTestValueBuildTool.prepareTag2("byteArray", bean1.byteArray!!).also {comp.add(it)}
            ApiTestValueBuildTool.iterableToListTag("strIterable", IdTagString, bean1.strIterable!!, ApiTestValueBuildTool::prepareTag).also {comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("longArray", bean1.longArray!!).also {comp.add(it)}
        }
        converterTemplate.expectedTag = comp2
        converterTemplate.assertNameNotEquals = true
        converterTemplate.assertValueNotEquals = true
        converterTemplate.apiTest(TestMnbt.inst.refConverterProxy, name1, name2, bean1, bean2, tk)

        // test only one value not equals
        bean2.byteArray = bean1.byteArray
        bean2.strIterable = bean1.strIterable
        bean2.longArray = bean1.longArray
        bean1.bytesIterable = ApiTestValueBuildTool.listPreparation(399) { Random.nextBytes(Random.nextInt(36, 95)) }
        ApiTestValueBuildTool.iterableToListTag("bytesIterable", IdTagByteArray, bean1.bytesIterable!!, ApiTestValueBuildTool::prepareTag).also {comp2.add(it)}
        converterTemplate.apiTest(TestMnbt.inst.refConverterProxy, name1, name2, bean1, bean2, tk)
    }

    @Test
    fun conversionTestForArrayContainsBeans() {
        val beansCreation:(num:Int)->Array<JavaBean> = { num->
            Array(num) {
                val bean = JavaBean()
                bean.bits = Random.nextBytes(Random.nextInt(15,50))
                bean.j = RandomValueTool.bitStrC(20)()
                bean.l = Random.nextLong()
                bean.d = Random.nextDouble()
                bean.b = Random.nextInt() > 0
                bean
            }
        }
        val arr1 = beansCreation(10)
        val arr2 = beansCreation(10)
        val name1 = "bean1"
        val name2 = "bean2"
        val tk = object: MTypeToken<Array<JavaBean>>() {}
        val listTag = ListTag<CompoundTag>(10, name1).also { listTag->
            arr1.onEach { bean->
                CompoundTag().also { comp->
                    ApiTestValueBuildTool.prepareTag2("bits", bean.bits!!).also {comp.add(it)}
                    ApiTestValueBuildTool.prepareTag2("j", bean.j!!).also {comp.add(it)}
                    ApiTestValueBuildTool.prepareTag2("l", bean.l!!).also {comp.add(it)}
                    ApiTestValueBuildTool.prepareTag2("d", bean.d!!).also {comp.add(it)}
                    ApiTestValueBuildTool.prepareTag2("b", bean.b!!).also {comp.add(it)}
                }.also {
                    listTag.add(it)
                }
            }
        }
        converterTemplate.expectedTag = listTag
        // set reflect type converter accept nullable properties
        TestMnbt.inst.refReflectiveConverter.returnObjectWithNullableProperties = true
        converterTemplate.apiTest(TestMnbt.inst.refConverterProxy, name1, name2, arr1, arr2, tk)
    }

    @Test
    fun inheritanceBeanTest() {
        val creation:()->JavaBean3 = {
            val bean3 = JavaBean3()
            bean3.bean3Int = Random.nextInt()
            bean3.bean3Str = "fix str"
            bean3.s = Random.nextInt().toShort()
            bean3.b = false
            bean3.bits = Random.nextBytes(100)
            bean3
        }
        val tk = object:MTypeToken<JavaBean3>() {}
        val bean1 = creation()
        val bean2 = creation()
        val name1 = "bean3-1"
        val name2 = "bean3-2"
        val compound = CompoundTag(name1).also { comp->
            ApiTestValueBuildTool.prepareTag2("bits", bean1.bits!!).also {comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("b", bean1.b!!).also {comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("s", bean1.s!!).also {comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("bean3Int", bean1.bean3Int!!).also {comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("bean3Str", bean1.bean3Str!!).also {comp.add(it)}
        }
        converterTemplate.expectedTag = compound
        converterTemplate.apiTest(TestMnbt.inst.refConverterProxy, name1, name2, bean1, bean2, tk)
    }


    @Test
    fun complicatedBeanTest() {
        val tk = object:MTypeToken<JavaBean4>() {}
        val bean1 = randomJavaBean4()
        val bean2 = randomJavaBean4()
        val name1 = "bean4-1"
        val name2 = "bean4-2"
        val compound = CompoundTag(name1).also { comp->
            ApiTestValueBuildTool.prepareTag2("bean4Var", bean1.bean4Var!!).also {comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("bean3Int", bean1.bean3Int!!).also {comp.add(it)}
            ApiTestValueBuildTool.prepareTag2("d", bean1.d!!).also {comp.add(it)}
            CompoundTag("beanMap").also { beanMap->
                val map = bean1.beanMap!!
                map.onEach { entry ->
                    beanMap.add(entry.value.toCompound(entry.key))
                }
                comp.add(beanMap)
            }
            ListTag<CompoundTag>(IdTagCompound,"beanList").also { listTag ->
                val subTk = object:MTypeToken<JavaBean>() {}
                bean1.beanList!!.onEach { subBean->
                    TestMnbt.inst.refConverterProxy.createTag(null, subBean, subTk)!!.also {listTag.add(it as CompoundTag)}
                }
            }.also {comp.add(it)}
        }
        converterTemplate.expectedTag = compound
        converterTemplate.apiTest(TestMnbt.inst.refConverterProxy, name1, name2, bean1, bean2, tk)
    }
}