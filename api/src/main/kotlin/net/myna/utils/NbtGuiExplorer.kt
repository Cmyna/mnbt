package net.myna.utils

import net.myna.mnbt.Tag
import java.awt.Rectangle
import java.io.File
import javax.swing.JFrame

// TODO
/**
 * a Nbt explorer base on java swing,
 * it can load,show and modify a region file or an nbt file
 */
class NbtGuiExplorer:JFrame() {

    private lateinit var nbtFile: File
    private lateinit var rootTag: Tag<out Any>
    private lateinit var regionLoader: RegionLoader

    fun loadFile(filePath:String) {
        TODO()
    }

    init {
        this.title = "Nbt Explorer"
        this.bounds = Rectangle(100, 100, 600, 800)
        this.defaultCloseOperation = EXIT_ON_CLOSE
    }

    companion object {
        @JvmStatic
        fun main(args:Array<String>) {
            // test Nbt explorer
            val explorer = NbtGuiExplorer()
            explorer.isVisible = true
        }
    }
}