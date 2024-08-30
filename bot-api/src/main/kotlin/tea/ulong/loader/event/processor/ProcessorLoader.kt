package tea.ulong.loader.event.processor

import tea.ulong.entity.event.processor.Prefix
import tea.ulong.entity.event.processor.Processor
import tea.ulong.entity.event.processor.ProcessorFun
import tea.ulong.entity.event.processor.ProcessorFunI
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.entity.event.processor.annotation.Processor as ProcessorAnnotation
import java.io.File
import java.net.URLClassLoader
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.jar.JarFile
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * Processor加载器,可以通过指定包加载包内的Processor,或者加载外部路径下的jar包中的Processor
 *
 * [loadInternalProcessor]与[loadExternalProcessor]由chatgpt-4o生成后修改而来
 */
object ProcessorLoader {

    private var _internalProcessor: List<Processor>? = null

    /**
     * 读取"tea.ulong.event.processor"包下的所有processor并生成代理对象.
     *
     * 内部使用缓存机制,无法立刻反应变化
     * @return processor代理对象列表
     */
    fun loadInternalProcessor(pack: String): List<Processor> {
        if (_internalProcessor != null) return _internalProcessor!!

        val packPath = pack.replace(".", "/")
        val classLoader = this.javaClass.classLoader
        val resource = classLoader.getResources(packPath).toList()

        _internalProcessor = resource.flatMap { url ->
            if (url.protocol == "jar") {
                // 处理JAR文件中的资源
                val jarPath = url.path.substringAfter("file:").substringBefore("!/")
                val jarFile = JarFile(URLDecoder.decode(jarPath, "UTF-8"))
                jarFile.entries().asSequence().filter { entry ->
                    entry.name.startsWith(packPath) && entry.name.endsWith(".class")
                }.map { entry ->
                    val className = entry.name.removeSuffix(".class").replace('/', '.')
                    classLoader.loadClass(className).kotlin
                }.toList()
            } else {
                // 处理文件系统中的资源
                val dirPath = URLDecoder.decode(url.path, "UTF-8").substring(1)
                val dir = File(dirPath)
                if (dir.exists() && dir.isDirectory) {
                    dir.walkTopDown().filter { it.isFile && it.extension == "class" }.map { file ->
                        val relativePath = Paths.get(dirPath).relativize(Paths.get(file.absolutePath)).toString()
                        val className = relativePath.removeSuffix(".class").replace(File.separatorChar, '.')
                        classLoader.loadClass("$pack.$className").kotlin
                    }.toList()
                } else {
                    emptyList()
                }
            }
        }.mapNotNull { clazz ->
            clazz.findAnnotation<tea.ulong.entity.event.processor.annotation.Processor>()?.let {
                Processor(clazz)
            }
        }

        DynamicContainers["internalProcessor".lowercase()] = _internalProcessor
        DynamicContainers["internalProcessor"] = _internalProcessor
        for (processor in _internalProcessor!!) {
            DynamicContainers[(processor.symbol?.value ?: processor.clazz.simpleName ?: "null").lowercase()] = processor
            DynamicContainers[processor.symbol?.value ?: processor.clazz.simpleName ?: "null"] = processor
        }

        return _internalProcessor!!
    }

    fun loadExternalProcessor(path: String): List<Processor> {
        val classesWithAnnotation = mutableListOf<Processor>()

        val pathFile = File(path)
        if (!pathFile.exists()){
            pathFile.mkdirs()
        }
        // 查找所有 JAR 文件
        val jarFiles = Files.walk(Paths.get(path))
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".jar") }
            .toList()

        // 加载 JAR 文件并创建类加载器
        val classLoader = URLClassLoader.newInstance(
            jarFiles.map { it.toUri().toURL() }.toTypedArray(),
            Thread.currentThread().contextClassLoader
        )

        jarFiles.forEach { jarFile ->
            JarFile(jarFile.toFile()).use { jar ->
                jar.entries().asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".class") }
                    .map { it.name.replace("/", ".").removeSuffix(".class") }
                    .forEach { className ->
                        try {
                            // 加载类并检查是否带有指定的注解
                            val clazz = classLoader.loadClass(className).kotlin
                            if (clazz.hasAnnotation<ProcessorAnnotation>()) {
                                classesWithAnnotation.add(Processor(clazz))
                            }
                        } catch (e: ClassNotFoundException) {
                            // 忽略无法加载的类
                        }
                    }
            }
        }

        DynamicContainers["externalProcessor".lowercase()] = classesWithAnnotation
        DynamicContainers["externalProcessor"] = classesWithAnnotation
        for (processor in classesWithAnnotation) {
            DynamicContainers[(processor.symbol?.value ?: processor.clazz.simpleName ?: "null").lowercase()] = processor
            DynamicContainers[processor.symbol?.value ?: processor.clazz.simpleName ?: "null"] = processor
        }

        return classesWithAnnotation
    }

    fun loadChainMap(pack: String = "tea.ulong.core.plugin", pluginPath: String = "plugin"): Pair<Map<Prefix, Map<String, List<ProcessorFunI>>>, Map<String, List<ProcessorFunI>>> {
        val processors = loadInternalProcessor(pack) + loadExternalProcessor(pluginPath)

        val processorFunMap = mutableMapOf<Prefix, MutableMap<String, MutableList<ProcessorFun>>>()
        val emptyPrefixProcessorFunMap = mutableMapOf<String, MutableList<ProcessorFun>>()
        val haveFrontProcessorFun = LinkedList<ProcessorFun>()

        for (processor in processors){
            for (func in processor.triggerFun){
                // 如果该函数没有前驱函数
                if (func.trigger.front.isEmpty()){
                    // 将函数链的头存入映射表
                    for (prefix in func.prefixs){
                        if (prefix.symbol == ""){
                            for (trigger in func.trigger.triggers){
                                emptyPrefixProcessorFunMap.getOrPut(trigger){ mutableListOf() }.add(func)
                            }
                            continue
                        }
                        val map = processorFunMap.getOrPut(prefix) { mutableMapOf() }
                        for (trigger in func.trigger.triggers){
                            map.getOrPut(trigger){ mutableListOf() }.add(func)
                        }
                    }
                    continue
                }
                haveFrontProcessorFun.offer(func)
            }
        }

        val processorFunList: List<ProcessorFun> = processorFunMap.values.flatMap { it.values }.flatten() + emptyPrefixProcessorFunMap.values.flatten() + haveFrontProcessorFun
        DynamicContainers["processorFunList".lowercase()] = processorFunList.distinct()
        DynamicContainers["processorFunList"] = DynamicContainers["processorFunList".lowercase()]

        for (item in processorFunList) {
            DynamicContainers[(item.symbol?.value ?: item.function.name).lowercase()] = item
            DynamicContainers[(item.symbol?.value ?: item.function.name)] = item
        }

        var func: ProcessorFun?
        while (haveFrontProcessorFun.poll().apply { func = this } != null){
            val assertFunc = func!!
            if (assertFunc.trigger.front.trim() == "*"){
                processorFunMap.values.forEach {
                    for (kv in it){
                        for (v in kv.value){
                            for (key in assertFunc.trigger.triggers){
                                v.next[key] = assertFunc
                            }
                            for (trigger in v.trigger.triggers){
                                assertFunc.prev[trigger] = v
                            }
                        }
                    }
                }
                continue
            }

            val queue = LinkedList<Pair<String, ProcessorFun>>()
            for (processorFun in processorFunMap.values){
                for (kv in processorFun){
                    for (v in kv.value){
                        queue.offer(v.funFullName to v)
                    }
                }
            }
            if (queue.isEmpty()) break
            var target = queue.pop()
            var isYes = false
            do {
                val flag = assertFunc.trigger.front == target.first
                if (flag){
                    for (key in assertFunc.trigger.triggers){
                        target.second.next[key] = assertFunc
                    }
                    for (trigger in target.second.trigger.triggers){
                        assertFunc.prev[trigger] = target.second
                    }
                    isYes = true
                }
                for (next in target.second.next) {
                    queue.offer(next.value.funFullName to next.value)
                }
            }while(!queue.isEmpty().apply { if (!this) target = queue.pop() })
            run {
                if (!isYes){
                    val subTarget = haveFrontProcessorFun.find { it.funFullName == assertFunc.trigger.front } ?: return@run
                    for (key in assertFunc.trigger.triggers){
                        subTarget.next[key] = assertFunc
                    }
                    for (trigger in subTarget.trigger.triggers){
                        assertFunc.prev[trigger] = subTarget
                    }
                }
            }
        }

        processors.forEach { it.init?.invoke() }

        return processorFunMap to emptyPrefixProcessorFunMap
    }
}