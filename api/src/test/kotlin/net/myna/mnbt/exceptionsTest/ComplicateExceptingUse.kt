package net.myna.mnbt.exceptionsTest

import org.junit.jupiter.api.Test

/**
 * list some un-expecting use case which may cause Exception throws.
 * aims to design a better (more readable/trackable) Exceptions throws when these use cases occurs
 */
class ComplicateUnExpectingUse {

    @Test
    /**
     * decode nbt data with wrong de-compress format
     */
    fun wrongCompressFormat() {
        // decode gzip format as un-compressed

        // decode zlib format as un-compressed

        // decode un-compressed format as gzip/zlib
        TODO()
    }

    @Test
    /**
     * try to decode nbt data from an non-binary nbt data
     */
    fun notNbtData() {

        TODO()
    }


}