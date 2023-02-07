package mnbt.utils

import com.myna.mnbt.Mnbt

class TestMnbt: Mnbt() {

    val mockConverterProxy = MockConverterProxy(super.converterProxy)
    val mockCodecProxy = MockCodecProxy(super.codecProxy)

    val refConverterProxy = super.converterProxy
    val refCodecProxy = super.codecProxy
    val refReflectiveConverter = super.reflectiveConverter
    val refMapConverter = super.mapTypeTagConverter
    val refArrayToListConverter = super.arrayTypeListTagConverter
    val refListConverter = super.listTypeConverter

    val refCompoundCodec = super.compoundTagCodec
    val refListCodec = super.listCodec

    init {
        super.reflectiveConverter.proxy = mockConverterProxy
        super.arrayTypeListTagConverter.proxy = mockConverterProxy
        super.mapTypeTagConverter.proxy = mockConverterProxy
        super.listTypeConverter.proxy = mockConverterProxy

        super.listCodec.proxy = mockCodecProxy
        super.compoundTagCodec.proxy = mockCodecProxy

        super.reflectiveConverter.printStacktrace = true
    }

    companion object {
        val inst = TestMnbt()
    }
}