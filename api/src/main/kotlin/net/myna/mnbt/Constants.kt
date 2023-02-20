package net.myna.mnbt



/**
 * tag head payload: 3 bytes(1 byte for id, two bytes for name len)
 */
const val TagHeadFixPayload = 3
/**
 * payload: 1 bytes
 */
const val TagIdPayload = 1

/**
 * payload: 4 bytes
 */
const val ArraySizePayload = 4

/**
 * payload: 2 bytes
 */
const val StringSizePayload = 2

/**
 * payload:4
 */
const val IntSizePayload = 4

/**
 * payload:1
 */
const val ByteSizePayload = 1

/**
 * payload: 2
 */
const val ShortSizePayload = 2

/**
 * payload: 8
 */
const val LongSizePayload = 8

/**
 * payload: 4
 */
const val FloatSizePayload = 4

/**
 * payload: 8
 */
const val DoubleSizePayload = 8

/**
 * id:0
 */
const val IdTagEnd:Byte = 0

/**
 * id:1
 */
const val IdTagByte:Byte = 1

/**
 * id:2
 */
const val IdTagShort:Byte = 2

/**
 * id:3
 */
const val IdTagInt:Byte = 3
/**
 * id:4
 */
const val IdTagLong:Byte = 4

/**
 * id:5
 */
const val IdTagFloat:Byte = 5

/**
 * id:6
 */
const val IdTagDouble:Byte = 6

/**
 * id:7
 */
const val IdTagByteArray:Byte = 7

/**
 * id:8
 */
const val IdTagString:Byte = 8

/**
 * id:9
 */
const val IdTagList:Byte = 9

/**
 * id:10
 */
const val IdTagCompound:Byte = 10

/**
 * id:11
 */
const val IdTagIntArray:Byte = 11

/**
 * id:12
 */
const val IdTagLongArray:Byte = 12

/**
 * default max depth:512
 */
const val defaultTreeDepthLimit = 512

val idNameMapper = mapOf(
        IdTagEnd to "End Tag",
        IdTagCompound to "Compound Tag",
        IdTagByteArray to "ByteArray Tag",
        IdTagByte to "Byte Tag",
        IdTagFloat to "Float Tag",
        IdTagShort to "Short Tag",
        IdTagString to "String Tag",
        IdTagLong to "Long Tag",
        IdTagInt to "Int Tag",
        IdTagList to "List Tag",
        IdTagLongArray to "LongArray Tag",
        IdTagDouble to "Double Tag",
        IdTagIntArray to "Int Array Tag",
)