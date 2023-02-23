package net.myna.mnbt;

import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.myna.Tools;
import net.myna.utils.RegionLoader;
import org.junit.jupiter.api.Test;
import kotlin.io.FilesKt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import static net.myna.utils.RegionsLoader.ZLIB;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RegionLoaderTest {

    public RegionLoaderTest() {
        try {
            this.testRegionFile = new File(
                    Objects.requireNonNull(this.getClass().getResource("/nbt_data/regions/r.3.4.mca")).toURI()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testApi() {
        Tools.INSTANCE.withTestDirScene(new Function1<File, Unit>() {
            @Override
            public Unit invoke(File file) {
                File tempRegionFile = FilesKt.resolve(file, "r.3.4.mca");
                FilesKt.copyTo(testRegionFile, tempRegionFile, false, 8192);
                RegionLoader loader = new RegionLoader(tempRegionFile);
                Mnbt mnbt = new Mnbt();
                int localChunkX = 16;
                int localChunkZ = 3;
                Pair<BufferedInputStream, Integer> pair = loader.getChunkBinaryInputStream(localChunkX, localChunkZ);
                assert pair != null;

                try {
                    Tag<?> tag = mnbt.decode(pair.getFirst());
                    assertNotNull(tag);
                    pair.getFirst().close();
                    byte[] bytes = mnbt.encode(tag);
                    loader.writeTargetChunkData(localChunkX, localChunkZ, bytes, ZLIB);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return null;
            }
        });
    }

    private final File testRegionFile;
}
