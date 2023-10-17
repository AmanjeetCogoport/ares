package com.cogoport.ares.model.payment.enum

enum class PaymentSageGLCodes(val glCode: String, val currency: String, val entityCode: Int) {
    RBLCP("222021", "INR", 301),
    RBLP("222013", "INR", 301),
    INDC("222015", "INR", 301),
    RBLPU("222017", "USD", 301),
    RAZO("", "INR", 301),
    RBLC("222001", "INR", 101),
    RBLD("314003", "INR", 101),
    RBLU("222002", "USD", 101),
    AXISP("222020", "INR", 301)
}
