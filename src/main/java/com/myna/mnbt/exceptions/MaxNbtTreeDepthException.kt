package com.myna.mnbt.exceptions

import java.lang.RuntimeException

class MaxNbtTreeDepthException(maxDepth:Int): RuntimeException() {
    override val message: String? = "nbt tree depth over maximum depth ($maxDepth)!"
}