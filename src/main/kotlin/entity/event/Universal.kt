package tea.ulong.entity.event

/**
 * 通用的等级标志,任何不带有特殊色彩的等级都应尽量使用该枚举类
 */
enum class UniversalLevel(val level: Int) {

    /**重要**/ Vital(100),
    /**普通**/ Ordinary(50),
    /**忽视**/ UnVital(0);

}