package com.cogoport.ares.common.enum

enum class Quarter(
    var quarter: String,
    var months: MutableList<String>
) {
    Q1("Q1", mutableListOf("Jan", "Feb", "Mar")),
    Q2("Q2", mutableListOf("Apr", "May", "Jun")),
    Q3("Q3", mutableListOf("Jul", "Aug", "Sep")),
    Q4("Q4", mutableListOf("Oct", "Nov", "Dec"));

    fun getMonth(): MutableList<String > {
        return this.months
    }

    fun getQuarter(): MutableList<String > {
        return this.months
    }
}
