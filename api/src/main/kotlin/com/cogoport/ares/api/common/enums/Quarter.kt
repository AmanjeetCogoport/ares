package com.cogoport.ares.api.common.enums

enum class Quarter(
    var quarter: Int,
    var months: MutableList<String>
) {
    Q1(1, mutableListOf("Jan", "Feb", "Mar")),
    Q2(2, mutableListOf("Apr", "May", "Jun")),
    Q3(3, mutableListOf("Jul", "Aug", "Sep")),
    Q4(4, mutableListOf("Oct", "Nov", "Dec"));

    fun getMonth(): MutableList<String > {
        return this.months
    }

    fun getQuarter(): MutableList<String > {
        return this.months
    }
}
