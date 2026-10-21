package com.example.data.local.entity

enum class ItemStatus(val label: String) {
    PENDING("Pendente"),
    PARTIAL("Parcial"),
    PAID("Quitado"),
    OVERDUE("Vencido")
}

enum class InterestType(val label: String, val shortLabel: String) {
    MONTHLY("Ao Mês", "A.M."),
    DAILY("Ao Dia", "A.D.")
}

enum class ReceivableCategory(val label: String) {
    SALE("Venda"),
    SERVICE("Serviço"),
    PRODUCT("Produto"),
    OTHER("Outros")
}

enum class PayableCategory(val label: String) {
    SUPPLIER("Fornecedor"),
    SERVICE("Serviço"),
    BILLS("Contas"),
    OTHER("Outros")
}
