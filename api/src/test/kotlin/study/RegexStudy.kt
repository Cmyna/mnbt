package study

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegexStudy {

    @Test
    fun anyStringMatchTest() {
        val regex = Regex("^(\\S|\\s)+$")

        regex.assertStudy1(" ")
        regex.assertStudy1("ass ")
        regex.assertStudy1("ass.. ")
        regex.assertStudy1("as123s.. ")
        regex.assertStudy1("\n\t")
        regex.assertStudy1("ass..\\v ")
        regex.assertStudy1("ass.. utf8字符")
    }

    @Test
    fun matchFirstPackage() {
        //val regex = Regex("^(?:(\\\\\\.)|[^\\.])+(?=\\.)") //
        val regex = Regex("^([^\\.]*\\\\.)*[^\\.]*")
        regex.assertStudy2("pack1.pack2.pack3", "pack1")
        regex.assertStudy2("\npac k1.pack2.pack3", "\npac k1")
        regex.assertStudy2("ased", "ased")
        regex.assertStudy2("\\utf8中文@字符\n\t\r\b.*", "\\utf8中文@字符\n\t\r\b")
        regex.assertStudy2("a\\.bc.ddd", "a.bc")
        regex.assertStudy2("name\\..", "name.")
        regex.assertStudy2("aaa\\.bbb\\.ccc.ddd\\.ee..", "aaa.bbb.ccc")

    }


    private fun Regex.assertStudy1(str:String) {
        assertEquals(str, this.find(str,0)?.value)
    }

    private fun Regex.assertStudy2(str:String, str2:String?) {
        val res = this.find(str,0)?.value?.let {
            val list = it.split("\\.")
            if (list.size == 1) list[0]
            else {
                val builder = StringBuilder()
                list.onEachIndexed { i,str->
                    if (i<list.size-1) builder.append("$str.")
                    else builder.append(str)
                }
                builder.toString()
            }
        }
        println(res)
        assertEquals(str2, res)
        println("------------------------------------------")
    }
}