package tea.ulong.entity.event

enum class UniversalLevel(val level: Int) {

    /**重要**/ Vital(100),
    /**普通**/ Ordinary(50),
    /**忽视**/ UnVital(0);

}