package profiling.mnbt

import com.myna.mnbt.Mnbt
import mnbt.utils.ApiTestValueBuildTool
import mnbt.utils.TestMnbt
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit


@State(Scope.Thread)
@Measurement(iterations = 200, time = 2, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
open class CodecBenchmark {

    val mnbt = TestMnbt.inst
    val tagsList = ApiTestValueBuildTool.flatTagsPreparation(true)
    val outputStream = ByteArrayOutputStream()
    val decodeBytes = tagsList.map { mnbt.encode(it) }



    @Benchmark
    fun encodeToBytes(blackhole: Blackhole) {
        tagsList.onEach {
            blackhole.consume(mnbt.encode(it))
        }
    }

    @Benchmark
    fun encodeToStream(blackhole: Blackhole) {
        tagsList.onEach {
            outputStream.reset()
            mnbt.encode(it, outputStream)
            blackhole.consume(outputStream.toByteArray())
        }
    }

    @Benchmark
    fun decodeFromBytes(blackhole: Blackhole) {
        decodeBytes.onEach {
            blackhole.consume(mnbt.decode(it, 0))
        }
    }

    @Benchmark
    fun decodeFromStream(blackhole: Blackhole) {
        decodeBytes.onEach {
            val inputStream = ByteArrayInputStream(it)
            blackhole.consume(mnbt.decode(inputStream))
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val opt = OptionsBuilder()
                .include(CodecBenchmark::class.simpleName)
                .forks(1)
                .build()

            Runner(opt).run();
        }
    }
}