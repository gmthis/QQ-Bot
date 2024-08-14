package tea.ulong.loader.event.processor

import tea.ulong.entity.event.processor.Processor
import java.io.File
import java.net.URLDecoder
import java.nio.file.Paths
import java.util.jar.JarFile

/**
 * 内置Processor加载器,仅用于加载内置Processor,无法加载外置Processor
 *
 * 由chatgpt-4o生成
 */
object InternalProcessorLoader {

    private var _internalProcessor: List<Processor>? = null

    /**
     * 读取"tea.ulong.event.processor"包下的所有processor并生成代理对象.
     *
     * 内部使用缓存机制,无法立刻反应变化
     * @return processor代理对象列表
     */
    fun getInternalProcessorList(): List<Processor> {
        if (_internalProcessor != null) return _internalProcessor!!

        val pack = "tea.ulong.event.processor"
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
        }.map {
            Processor(it)
        }

        return _internalProcessor!!
    }

}