package entity.event.processor

import org.junit.jupiter.api.Test
import tea.ulong.entity.event.processor.Prefix
import tea.ulong.innerDynamicContainers
import kotlin.test.BeforeTest

class Test2{
    val result = "hello"
}

class Test{
    val test2: Test2 = Test2()
}

class PrefixTest{

    @BeforeTest
    fun before(){
        innerDynamicContainers["test"] = entity.event.processor.Test()
        innerDynamicContainers["test2"] = Test2()
        innerDynamicContainers["result"] = "hello"
    }

    @Test
    fun dynamicallyAcquiredTest(){
        val prefix = Prefix("#@test.test2.result@#")
        println(prefix.dynamicallyAcquired())
        assert(prefix.dynamicallyAcquired() == "hello")
        val prefix2 = Prefix("#@test2.result@#")
        println(prefix2.dynamicallyAcquired())
        assert(prefix2.dynamicallyAcquired() == "hello")
        val prefix3 = Prefix("#@result@#")
        println(prefix3.dynamicallyAcquired())
        assert(prefix3.dynamicallyAcquired() == "hello")
    }
}