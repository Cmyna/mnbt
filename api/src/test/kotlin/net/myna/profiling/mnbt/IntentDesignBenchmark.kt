package net.myna.profiling.mnbt

import org.assertj.core.internal.bytebuddy.utility.JavaConstant.Dynamic
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import java.io.InputStream
import java.lang.reflect.Proxy
import java.util.Deque
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.javaGetter

@State(Scope.Thread)
@Measurement(iterations = 50, time = 2, timeUnit = TimeUnit.MICROSECONDS)
@Warmup(iterations = 20, time = 1, timeUnit = TimeUnit.MICROSECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
open class IntentDesignBenchmark {

    val j = "some string"
    val dynamicProxyInst = Proxy.newProxyInstance(this::class.java.classLoader,
        arrayOf(Sub1::class.java, Sub2::class.java, Sub3::class.java)
    ) { _,method,_args ->
        return@newProxyInstance when(method) {
            Sub1::j.javaGetter -> j
            else -> null
        }
    } as Root
    val containerInst = object:interfaceContainer {
        override val set: Set<Root> = setOf(
            object:Sub1 {
                override val i: Int
                    get() = TODO("Not yet implemented")
                override val j: String = this@IntentDesignBenchmark.j
                override val k: Deque<Any>
                    get() = TODO("Not yet implemented")
                override val b: Boolean
                    get() = TODO("Not yet implemented")

            }
        )
    }

    @Benchmark
    fun createByDynamicProxies(blackhole:Blackhole) {
        val inst = Proxy.newProxyInstance(this::class.java.classLoader,
            arrayOf(Sub1::class.java, Sub2::class.java, Sub3::class.java)
        ) { _,method,_args ->
        } as Root
        blackhole.consume(inst)
    }

    @Benchmark
    fun createDirectly(blackhole: Blackhole) {
        val inst = object:Sub1, Sub2, Sub3 {
            override val i: Int
                get() = TODO("Not yet implemented")
            override val j: String
                get() = TODO("Not yet implemented")
            override val k: Deque<Any>
                get() = TODO("Not yet implemented")
            override val b: Boolean
                get() = TODO("Not yet implemented")
            override val obj: Any
                get() = TODO("Not yet implemented")
            override val inputStream: InputStream
                get() = TODO("Not yet implemented")

        } as Root
        blackhole.consume(inst)
    }

    @Benchmark
    fun createWithContainer(blackhole: Blackhole) {
        val sub1 = object:Sub1 {
            override val i: Int
                get() = TODO("Not yet implemented")
            override val j: String
                get() = TODO("Not yet implemented")
            override val k: Deque<Any>
                get() = TODO("Not yet implemented")
            override val b: Boolean
                get() = TODO("Not yet implemented")

        }

        val sub2 = object:Sub2 {
            override val obj: Any
                get() = TODO("Not yet implemented")
        }

        val sub3 = object:Sub3 {
            override val inputStream: InputStream
                get() = TODO("Not yet implemented")
        }
        val inst = object:interfaceContainer {
            override val set = setOf(sub1, sub2, sub3)
        }
        blackhole.consume(inst)
    }

    @Benchmark
    fun accessValByProxyInst(blackhole: Blackhole) {
        (this.dynamicProxyInst as Sub1).j.also {blackhole.consume(it)}
    }

    @Benchmark
    fun accessByContainer(blackhole: Blackhole) {
        (this.containerInst.set.find { it is Sub1 } as Sub1).j.also {blackhole.consume(it)}
    }

    interface Root

    interface Sub1:Root {
        val i:Int
        val j:String
        val k:Deque<Any>
        val b:Boolean
    }

    interface Sub2:Root {
        val obj:Any
    }

    interface Sub3:Root {
        val inputStream: InputStream
    }

    interface interfaceContainer{
        val set:Set<Root>
    }

    companion object {

        @JvmStatic
        fun main(args:Array<String>) {
            val opt = OptionsBuilder()
                .include(IntentDesignBenchmark::class.simpleName)
                .forks(1)
                .build()

            Runner(opt).run()
        }
    }
}