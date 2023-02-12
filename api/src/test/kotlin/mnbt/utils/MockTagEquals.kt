package mnbt.utils

import com.myna.mnbt.*
import com.myna.mnbt.tag.CompoundTag
import com.myna.mnbt.tag.ListTag
import java.lang.IllegalStateException
import java.lang.StringBuilder

/**
 * this tool mocks the Tag equals function and 'inject' log recording code in equals function.
 *
 * this mock also use on-the-fly checking that if mock equals result comes difference between original tag equals result,
 * it will throws [IllegalStateException]
 */
class MockTagEquals {

    private val logBuilder = StringBuilder()



    fun cleanBuild() {
        logBuilder.clear()
    }

    fun equals(tag:Tag<out Any>, other: Any?):Boolean {
        val currentNode = LogTreeNode(tag, other, null, tag.name?:"#")
        return equalsOrContains(tag, other, currentNode, false)
    }

    /**
     * specifies that source contains or equals other.
     * if source and other has compound tag at same nbt path, source compound tag has all key-values also contains or equals to key-values in other's;
     * the compound tag from source may have extra key-values not appears in other's relate compound tag
     *
     * if source and other has list tag at same nbt path, source list has all elements contains or equals to other's list tag's elements at same index;
     * the list tag from source may have more elements than other's relate list tag
     * */
    fun equalsOrContains(source:Tag<out Any>, other:Any?):Boolean {
        val currentNode = LogTreeNode(source, other, null, source.name?:"#")
        return equalsOrContains(source, other, currentNode, true)
    }

    private fun equalsOrContains(source: Tag<out Any>, other:Any?, currentNode: LogTreeNode, eqOrContains:Boolean):Boolean {
        val isEqInOriginEq = source==other
        // compare tag type, id, name, if not equals, return false
        val mockIsTag = isTagAndEqName(source, other, currentNode)
        if (!mockIsTag) {
            if (isEqInOriginEq) throwMockException(source, other, mockIsTag, isEqInOriginEq)
            return false
        }
        other as Tag<out Any>
        // create current node

        val mockEqualResult = when(source) {
            is CompoundTag -> equalsWithLogs(source, other as CompoundTag, currentNode, eqOrContains)
            is ListTag<*> -> equalsWithLogs(source, other as ListTag<out Any>, currentNode, eqOrContains)
            else -> simpleEqualsWithLog(source, other, currentNode)
        }
        checkMockResult(mockEqualResult, isEqInOriginEq, source, other, eqOrContains)

        return mockEqualResult
    }

    private fun simpleEqualsWithLog(tag: Tag<out Any>, other: Tag<out Any>, currentNode: LogTreeNode?):Boolean {
        val originalEqRes = tag==other
        if (!originalEqRes) {
            val path = buildPath(currentNode)
            val msgBuilder = StringBuilder("tag value not match in comparing $tag and $other\n")
            msgBuilder.append("\tDifference Found At: $path (tag name: ${tag.name}) where tag id is ${tag.id}\n")
            msgBuilder.append("\tBecause it is a simple comparison, no other message out\n")
            outputMsg(msgBuilder)
        }
        return originalEqRes
    }


    private fun equalsWithLogs(tag: CompoundTag, other:CompoundTag, currentNode: LogTreeNode?, eqOrContains: Boolean = false):Boolean {
        val path = buildPath(currentNode)
        // firstly compare key set, found key set difference
        val tagExtraKeys = tag.value.keys.filter { !other.value.keys.contains(it) }
        val otherExtraKeys = other.value.keys.filter { !tag.value.keys.contains(it) }
        val keyEq = (eqOrContains || tagExtraKeys.isEmpty()) && otherExtraKeys.isEmpty()

        if (!keyEq) {
            val msgBuilder = StringBuilder("CompoundTag keys Not Equals\n")
            msgBuilder.append(getPathInfoForNeq(tag, path))
            if (otherExtraKeys.isNotEmpty()) {
                val keysStr = otherExtraKeys.fold("") { last,cur-> "$last, $cur"}
                msgBuilder.append("\ttag value miss these keys: [$keysStr]\n")
            }
            if (eqOrContains || tagExtraKeys.isNotEmpty()) {
                val keysStr = tagExtraKeys.fold("") { last,cur-> "$last, $cur"}
                msgBuilder.append("\tother value miss these keys: [$keysStr]\n")
            }
            outputMsg(msgBuilder)
        }

        // then compare each tag in tags with others
        var valueCeq = true // value contains or equals
        val listOfNeq = ArrayList<Tag<out Any>>()
        tag.value.forEach {
            val otherSubTag = other[it.key]?: return@forEach
            val subTagNode = LogTreeNode(it.value, other[it.key], currentNode, it.key)
            val subTagEq = equalsOrContains(it.value, otherSubTag, subTagNode, eqOrContains)
            if (!subTagEq && valueCeq) valueCeq = false
            if (!subTagEq) {
                listOfNeq.add(it.value)
            }
        }
        return keyEq && valueCeq
    }

    private fun equalsWithLogs(source: ListTag<out Any>, other: ListTag<out Any>, currentNode: LogTreeNode?, eqOrContains: Boolean):Boolean {
        // first compare size
        //val sizeEq = source.value.size == other.value.size
        val path = buildPath(currentNode)
        val msgBuilder = StringBuilder()
        val sizeMatch = if (eqOrContains) source.value.size >= other.value.size else source.value.size == other.value.size
        if (!sizeMatch) {
            msgBuilder.append("List Tag list size not match\n")
            msgBuilder.append("\tExpect size ${source.value.size}, but other got ${other.value.size}\n")
            msgBuilder.append(getPathInfoForNeq(source, path))
            outputMsg(msgBuilder)
            return false
        }
        // compare each element
        other.value.onEachIndexed { i, e ->
            val subTagNode = LogTreeNode(source.value[i], e, currentNode, "#indexAt[$i]")
            val res = equalsOrContains(source.value[i], e, subTagNode, eqOrContains)
            if (!res) {
                return false
            }
        }
        return true
    }

    private fun isTagAndEqName(tag:Tag<out Any>, other: Any?, currentNode: LogTreeNode):Boolean {
        val isTag = other is Tag<*>
        val sameId = if (other is Tag<*>) tag.id==other.id else false
        val sameName = if (other is Tag<*>) tag.name==other.name else false
        val sameClass = if (other != null) tag::class.java==other::class.java else false
        val match = isTag && sameId && sameName && sameClass
        if (!match) { // write message output
            val path = buildPath(currentNode)
            val msgBuilder = StringBuilder("tag type/name/id not match\n")
            msgBuilder.append("\tExpect $tag (tag name: ${tag.name}, id: ${tag.id} (${idNameMapper[tag.id]})) \n" +
                    "\t\tBut got $other ${if (other is Tag<*>) "(tag name: ${other.name}, id: ${other.id} (${idNameMapper[other.id]}))" else ""}\n")
            msgBuilder.append(getPathInfoForNeq(tag, path))
            if (!isTag) msgBuilder.append("\tOther is not a Tag (null or other object)\n")
            else msgBuilder.append("\tId Match: $sameId; Name Match:$sameName; Class Match: $sameClass;\n")
            outputMsg(msgBuilder)
        }
        return match
    }

    private fun outputMsg(builder:StringBuilder) {
        // TODO: may change other way to output message
        println(builder.toString())
    }

    private fun buildPath(startNode:LogTreeNode?):String {
        val parentList = buildPath(ArrayList(), startNode)
        parentList.reverse()
        return parentList.fold("{#empty}") { last,cur-> "$last->{$cur}" }
    }

    private fun buildPath(pathList: ArrayList<String>, currentNode: LogTreeNode?):ArrayList<String> {
        if (currentNode == null) return pathList // has reached root
        // if path list is null, means it is entry call in this recursive function
        pathList.add(currentNode.pathNameAlias) // if name is null, use '#' instead
        return buildPath(pathList, currentNode.thisParent)
    }

    private fun checkMockResult(mockEqualResult:Boolean, originalEqualResult:Boolean, tag: Tag<out Any>, other: Any?, eqOrContains:Boolean) {
        if (eqOrContains && mockEqualResult && !originalEqualResult) return
        if (mockEqualResult != originalEqualResult) throwMockException(tag, other, mockEqualResult, originalEqualResult)
    }

    private fun getPathInfoForNeq(tag:Tag<out Any>, path:String):String {
        return "\tDifference Found At: $path where tag id is ${tag.id}\n"
    }

    private fun throwMockException(tag: Tag<out Any>, other: Any?, mockRes:Boolean, actual:Boolean) {
        throw IllegalStateException("mock equals result($mockRes) not equals to original tag equals($actual)!\n When comparing $tag with $other")
    }

    /**
     * log tree node for recording upper tag comparison.
     *
     * it records true result that thisTag and otherTag has same class type, same name and same id, so does their parents.
     * but their value equals or not is uncertain.
     *
     * @param thisParent record direct parent holds this node, if null then this node is regarded as a root
     */
    data class LogTreeNode(val thisTag:Tag<out Any>, val otherTag:Any?, val thisParent: LogTreeNode?, val pathNameAlias:String)
}