package net.myna.utils

import net.myna.mnbt.AnyTag
import net.myna.mnbt.Mnbt
import net.myna.mnbt.Tag
import net.myna.mnbt.tag.CompoundTag
import net.myna.mnbt.tag.ListTag
import java.awt.Rectangle
import java.awt.ScrollPane
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * An Nbt Explorer Example base on java swing,
 * it can load and show a region file or an nbt file
 */
class NbtGuiExplorerExample:JFrame() {

    @Volatile private var file: File? = null
    @Volatile private var rootTag: Tag<out Any>? = null
    @Volatile private var regionLoader: RegionLoader? = null

    private var fileMenu = JMenu("file")
    private var menuItemOpen = JMenuItem("open")
    private var menuItemSave = JMenuItem("save")
    private var menuItemSaveAs = JMenuItem("save as")

    @Volatile private var nbtDisplayTree = JTree()

    private val mnbt = Mnbt()

    private fun loadNbt(file:File):Tag<out Any>? {
        // try no compress
        try {
            val inputStream = BufferedInputStream(FileInputStream(file))
            println("try load no compress")
            return mnbt.decode(inputStream)
        } catch (_: Exception) { println("load nbt file with no compress failed") }

        // try de-compress with gzip format
        try {
            val inputStream = BufferedInputStream(GZIPInputStream(FileInputStream(file)))
            println("try load gzip")
            return mnbt.decode(inputStream)
        } catch (_:Exception) { println("load nbt file with gzip compress failed") }

        // try de-compress with z-lib format
        try {
            val inputStream = BufferedInputStream(DeflaterInputStream(FileInputStream(file), Deflater()))
            println("try load z-lib")
            return mnbt.decode(inputStream)
        } catch (_:Exception) { println("load nbt file with z-lib compress failed") }

        // else return null
        return null
    }

    private fun launchFileLoading() {
        val file = this.file
        if (file!=null) {
            Thread {
                if (regionFileRegex.matchEntire(file.name)!=null) {
                    this.regionLoader = RegionLoader(file)
                } else {
                    this.rootTag = loadNbt(file)
                }
                // analyze regionLoader/nbt tag
                updateWholeJTree()
            }.start()
        }
    }


    private fun updateWholeJTree() {
        println("start update whole JTree")
        if (this.rootTag!=null) {
            println("update from root tag")
            val root = buildJTreeModel(this.rootTag!!)
            val model = DefaultTreeModel(root)
            SwingUtilities.invokeLater {
                this.nbtDisplayTree.model = model
                println("updated JTree model")
            }
        } else if (this.regionLoader != null) {
            println("update from region loader")
            val loader = this.regionLoader!!
            val chunkPosList = loader.listChunks()
            val root = DefaultMutableTreeNode("region data")
            chunkPosList.forEach {
                val (stream, length) = loader.getChunkBinaryInputStream(it.first, it.second)!!
                val chunk = mnbt.decode(stream)
                val chunkJNode = buildJTreeModel(chunk)
                chunkJNode.userObject = "[chunk at x:${it.first}, z:${it.second}]"
                root.add(chunkJNode)
            }
            val model = DefaultTreeModel(root)
            SwingUtilities.invokeLater {
                this.nbtDisplayTree.model = model
                println("updated JTree model")
            }
        }
    }



    private fun initMenuItemEvent() {
        this.menuItemOpen.addActionListener { event->
            val fileChooser = JFileChooser()
            val result = fileChooser.showOpenDialog(this)
            if (result == JFileChooser.APPROVE_OPTION) {
                this.file = fileChooser.selectedFile
                println("Selected file: ${this.file!!.name}")
                launchFileLoading()
            }
        }
        this.menuItemSave.addActionListener { event->
            if (this.regionLoader!=null) {
                TODO()
            } else if (this.rootTag != null) {
                TODO()
            }
        }
        this.menuItemSaveAs.addActionListener { event->
            if (this.regionLoader!=null) {
                TODO()
            } else if (this.rootTag != null) {
                TODO()
            }
        }
    }

    private fun buildJTreeModel(tag: Tag<out Any>):DefaultMutableTreeNode {
        if (tag is CompoundTag) {
            val rootNode = DefaultMutableTreeNode("${tag.name} (CompoundTag)")
            tag.value.onEach {
                val child = buildJTreeModel(it.value)
                rootNode.add(child)
            }
            return rootNode

        } else if (tag is ListTag<*>) {
            val rootNode = DefaultMutableTreeNode("${tag.name} (ListTag)")
            tag.value.onEach {
                val child = buildJTreeModel(it)
                rootNode.add(child)
            }
            return rootNode
        }
        return DefaultMutableTreeNode("$tag")
    }



    init {
        this.title = "Nbt Explorer"
        this.bounds = Rectangle(100, 100, 600, 800)
        this.defaultCloseOperation = EXIT_ON_CLOSE

        this.fileMenu.add(this.menuItemSave)
        this.fileMenu.add(this.menuItemOpen)
        this.fileMenu.add(this.menuItemSaveAs)
        this.jMenuBar = JMenuBar()
        this.jMenuBar.add(fileMenu)

        this.contentPane.add(JScrollPane(this.nbtDisplayTree))
        initMenuItemEvent()
    }

    private val regionFileRegex = Regex("""
        r\.\d+\.\d+\.mca
    """.trimIndent())

    companion object {
        @JvmStatic
        fun main(args:Array<String>) {
            // test Nbt explorer
            val explorer = NbtGuiExplorerExample()
            explorer.isVisible = true


        }
    }
}