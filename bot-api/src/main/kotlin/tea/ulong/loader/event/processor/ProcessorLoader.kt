package tea.ulong.loader.event.processor

import tea.ulong.entity.event.processor.Processor
import tea.ulong.entity.event.processor.annotation.Processor as ProcessorAnnotation
import java.io.File
import java.net.URLClassLoader
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.reflect.full.hasAnnotation

/**
 * 内置Processor加载器,仅用于加载内置Processor,无法加载外置Processor
 *
 * 由chatgpt-4o生成
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
        }.map {
            Processor(it)
        }

        return _internalProcessor!!
    }

    fun loadExternalProcessor(path: String): List<Processor> {
        val classesWithAnnotation = mutableListOf<Processor>()

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

        return classesWithAnnotation
    }
}