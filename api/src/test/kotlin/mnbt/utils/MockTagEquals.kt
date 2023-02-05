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
       return _equals(tag, other, currentNode)
    }

    private fun _equals(tag: Tag<out Any>, other:Any?, currentNode: LogTreeNode):Boolean {
        val isEqInOriginEq = tag==other
        // compare tag type, id, name, if not equals, return false
        if (!isTagAndEqName(tag, other, currentNode)) {
            if (isEqInOriginEq) throwMockException(tag, other)
            return false
        }
        other as Tag<out Any>
        // create current node

        val mockEqualResult = when(tag) {
            is CompoundTag -> equalsWithLogs(tag, other as CompoundTag, currentNode)
            is ListTag<*> -> equalsWithLogs(tag, other as ListTag<out Any>, currentNode)
            else -> simpleEqualsWithLog(tag, other, currentNode)
        }
        checkMockResult(mockEqualResult, isEqInOriginEq, tag, other)

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


    private fun equalsWithLogs(tag: CompoundTag, other:CompoundTag, currentNode: LogTreeNode?):Boolean {
        val path = buildPath(currentNode)
        // firstly compare key set, found key set difference
        val tagExtraKeys = tag.value.keys.filter { !other.value.keys.contains(it) }
        val otherExtraKeys = other.value.keys.filter { !tag.value.keys.contains(it) }
        val keyEq = tagExtraKeys.isEmpty() && otherExtraKeys.isEmpty()
        if (!keyEq) {
            val msgBuilder = StringBuilder("CompoundTag keys Not Equals\n")
            msgBuilder.append(getPathInfoForNeq(tag, path))
            if (otherExtraKeys.isNotEmpty()) {
                val keysStr = otherExtraKeys.fold("") { last,cur-> "$last, $cur"}
                msgBuilder.append("\ttag value miss these keys: [$keysStr]\n")
            }
            if (tagExtraKeys.isNotEmpty()) {
                val keysStr = tagExtraKeys.fold("") { last,cur-> "$last, $cur"}
                msgBuilder.append("\tother value miss these keys: [$keysStr]\n")
            }
            outputMsg(msgBuilder)
        }

        // then compare each tag in tags with others
        var valueEq = true
        val listOfNeq = ArrayList<Tag<out Any>>()
        tag.value.forEach {
            val otherSubTag = other[it.key]?: return@forEach
            val subTagNode = LogTreeNode(it.value, other[it.key], currentNode, it.key)
            val subTagEq = _equals(it.value, otherSubTag, subTagNode)
            if (!subTagEq && valueEq) valueEq = false
            if (!subTagEq) {
                listOfNeq.add(it.value)
            }
        }
//        if (valueEq && keyEq) return true
//        else {
//            val msgBuilder = StringBuilder("CompoundTag Not Equals\n")
//            msgBuilder.append(getPathInfoForNeq(tag, path))
//            if (!valueEq) {
//                val elementsNeqStr = listOfNeq.fold("") {last,cur-> "$last, ${cur.name}"}
//                msgBuilder.append("\tTag's sub tags not equals to other's\n")
//                msgBuilder.append("\tThose sub tags not equals: $elementsNeqStr\n")
//            }
//            return false
//        }
        return keyEq && valueEq
    }

    private fun equalsWithLogs(tag: ListTag<out Any>, other: ListTag<out Any>, currentNode: LogTreeNode?):Boolean {
        // first compare size
        val sizeEq = tag.value.size == other.value.size
        val path = buildPath(currentNode)
        val msgBuilder = StringBuilder()
        if (!sizeEq) {
            msgBuilder.append("List Tag list size not match\n")
            msgBuilder.append("\tExpect size ${tag.value.size}, but other got ${other.value.size}\n")
            msgBuilder.append(getPathInfoForNeq(tag, path))
            outputMsg(msgBuilder)
            return false
        }
        // compare each element
        tag.value.onEachIndexed { i, e ->
            val subTagNode = LogTreeNode(e, other.value[i], currentNode, "#indexAt[$i]")
            val res = _equals(e, other.value[i], subTagNode)
            if (!res) {
//                msgBuilder.append("List Tag element not match at index [$i]\n")
//                msgBuilder.append("\t element not equals to other's element at same index\n")
//                msgBuilder.append(getPathInfoForNeq(tag, path))
//                outputMsg(msgBuilder)
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

    private fun checkMockResult(mockEqualResult:Boolean, originalEqualResult:Boolean, tag: Tag<out Any>, other: Any?) {
        if (mockEqualResult != originalEqualResult) {
            throwMockException(tag, other)
        }
    }

    private fun getPathInfoForNeq(tag:Tag<out Any>, path:String):String {
        return "\tDifference Found At: $path where tag id is ${tag.id}\n"
    }

    private fun throwMockException(tag: Tag<out Any>, other: Any?) {
        throw IllegalStateException("mock equals result not equals to original tag equals! When comparing $tag with $other")
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