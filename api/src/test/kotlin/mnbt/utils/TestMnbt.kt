package mnbt.utils

import com.myna.mnbt.Mnbt

class TestMnbt: Mnbt() {

    val asserterConverterProxy = AsserterConverterProxy(super.converterProxy)
    val asserterCodecProxy = AsserterCodecProxy(super.codecProxy)

    val refConverterProxy = super.converterProxy
    val refCodecProxy = super.codecProxy
    val refReflectiveConverter = super.reflectiveConverter
    val refMapConverter = super.mapTypeTagConverter
    val refArrayToListConverter = super.arrayTypeListTagConverter
    val refListConverter = super.listTypeConverter

    val refCompoundCodec = super.compoundTagCodec
    val refListCodec = super.listCodec

    init {
        super.reflectiveConverter.proxy = asserterConverterProxy
        super.arrayTypeListTagConverter.proxy = asserterConverterProxy
        super.mapTypeTagConverter.proxy = asserterConverterProxy
        super.listTypeConverter.proxy = asserterConverterProxy

        super.listCodec.proxy = asserterCodecProxy
        super.compoundTagCodec.proxy = asserterCodecProxy

        super.reflectiveConverter.outputDebugInfo = true
    }

    companion object {
        val inst = TestMnbt()
    }
}