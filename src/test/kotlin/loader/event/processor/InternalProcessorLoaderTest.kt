package loader.event.processor

import org.junit.jupiter.api.Test
import tea.ulong.loader.event.processor.InternalProcessorLoader


class InternalProcessorLoaderTest{

    @Test
    fun getInternalProcessorListTest(){
        val result = InternalProcessorLoader.getInternalProcessorList()
        assert(result.isNotEmpty())
    }

}