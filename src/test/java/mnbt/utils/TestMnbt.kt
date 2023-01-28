package mnbt.utils

import com.myna.mnbt.Mnbt

class TestMnbt: Mnbt() {

    val asserterConverterProxy = AsserterConverterProxy(super.converterProxy)
    val asserterCodecProxy = AsserterCodecProxy(super.codecProxy)

    val tConverterProxy = super.converterProxy
    val tCodecProxy = super.codecProxy
    val tReflectiveConverter = super.reflectiveConverter
    val tMapConverter = super.mapTypeTagConverter
    val tArrayToListConverter = super.arrayTypeListTagConverter
    val tListConverter = super.listTypeConverter

    val tCompoundCodec = super.compoundTagCodec
    val tListCodec = super.listCodec

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