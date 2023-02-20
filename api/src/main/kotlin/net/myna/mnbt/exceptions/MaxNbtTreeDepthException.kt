package net.myna.mnbt.exceptions

import java.lang.RuntimeException

class MaxNbtTreeDepthException(maxDepth:Int, extraMsg:String = ""): RuntimeException() {
    override val message: String? = "$extraMsg; nbt tree depth over maximum depth ($maxDepth)!"
}