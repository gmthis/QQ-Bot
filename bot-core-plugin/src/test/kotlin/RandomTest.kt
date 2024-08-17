import kotlin.random.Random

private fun getRandom(): String {
    return when (Random.nextInt(10000000)) {
        // 0.00001%
        in 0..0 -> "欧皇"
        // 0.05%
        in 1..5000 -> "大胸"
        // 0.05%
        in 5001..10000 -> "平胸"
        // 0.05%
        in 10001..15000 -> "小吉吉"
        // 0.05%
        in 15001..20000 -> "大吉吉"
        // 0.05%
        in 20001..25000 -> "鸡"
        // 0.05%
        in 25001..30000 -> "稀有"
        // 0.05%
        in 30001..35000 -> "不稀有"
        // 0.05%
        in 35001..40000 -> "man"
        // 0.05%
        in 40001..45000 -> "好似"
        // 0.05%
        in 45001..50000 -> "雪豹"
        // 0.05%
        in 50001..55000 -> "理塘"
        else -> "其他结果"
    }
}

fun main() {
    // 统计各结果出现次数
    val counts = mutableMapOf(
        "欧皇" to 0,
        "大胸" to 0,
        "平胸" to 0,
        "小吉吉" to 0,
        "大吉吉" to 0,
        "鸡" to 0,
        "稀有" to 0,
        "不稀有" to 0,
        "man" to 0,
        "好似" to 0,
        "雪豹" to 0,
        "理塘" to 0,
        "其他结果" to 0
    )

    val totalIterations = 100000000

    // 执行一千万次
    repeat(totalIterations) {
        val result = getRandom()
        counts[result] = counts[result]!! + 1
    }

    // 打印结果统计
    counts.forEach { (result, count) ->
        val percentage = (count.toDouble() / totalIterations) * 100
        println("$result: $count (${String.format("%.5f", percentage)}%)")
    }
}