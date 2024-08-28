package tea.ulong.event.processor

import oshi.SystemInfo
import tea.ulong.entity.event.processor.annotation.Authentication
import tea.ulong.entity.event.processor.annotation.Authority
import tea.ulong.entity.event.processor.annotation.Processor
import tea.ulong.entity.event.processor.annotation.Trigger
import tea.ulong.ext.format
import java.lang.management.ManagementFactory

@Processor
class CoreEventProcessor {

    @Trigger("rs", "运行状态", "状态", "status", "srs")
    @Authentication(Authority.User)
    @Help("获取服务器运行状态.")
    fun getRunningStatus(): String{
        val systemInfo = SystemInfo()
        // 获取操作系统的扩展MXBean
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        // 获取系统版本
        val osVersion = System.getProperty("os.name") + " " + System.getProperty("os.version")
        // 获取JVM版本
        val jvmVersion = System.getProperty("java.version")

        // 获取CPU占用率 (兼容Linux和Windows)
        val cpuLoad = if (osBean is com.sun.management.OperatingSystemMXBean) {
            (osBean.cpuLoad * 100).format(2) + "%"
        } else {
            "N/A"
        }
        // 获取总内存 (兼容Linux和Windows)
        val totalMemory = if (osBean is com.sun.management.OperatingSystemMXBean) {
            (osBean.totalMemorySize / 1024 / 1024).format(2) + " MB"
        } else {
            "N/A"
        }
        // 获取JVM占用内存
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        val jvmUsedMemory = (memoryMXBean.heapMemoryUsage.used / 1024 / 1024 / 1024).format(2) + " GB"
        // 获取总使用内存 (兼容Linux和Windows)
        val totalUsedMemory = if (osBean is com.sun.management.OperatingSystemMXBean) {
            ((osBean.totalMemorySize - osBean.totalMemorySize) / 1024 / 1024 / 1024).format(2) + " GB"
        } else {
            "N/A"
        }

        val memory = systemInfo.hardware.memory
        val totalByte = memory.total
        val acaliableByte = memory.available

        // 输出格式化的信息
        return """系统版本: $osVersion
            |运行时: jvm-$jvmVersion
            |总内存: $totalMemory
            |Bot占用内存: $jvmUsedMemory
            |内存总使用: ${(((totalByte-acaliableByte)*1.0) / 1024 / 1024 /1024).format(2) + " GB"}
        """.trimMargin()
    }

}