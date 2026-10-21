package com.example.model

enum class FilterOption(val label: String) {
    ALL("Todos"),
    TODAY("Hoje"),
    THIS_WEEK("Esta Semana"),
    THIS_MONTH("Este Mês"),
    OVERDUE("Vencidos"),
    NO_DUE_DATE("Sem Prazo"),
    PENDING("Pendentes"),
    PAID("Quitados")
}
