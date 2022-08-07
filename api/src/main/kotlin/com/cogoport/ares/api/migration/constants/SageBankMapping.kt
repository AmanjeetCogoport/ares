package com.cogoport.ares.api.migration.constants

import com.cogoport.ares.api.migration.model.CogoBankInfo
import java.util.UUID

class SageBankMapping {

    fun getBankInfoByCode(code: String): CogoBankInfo? {

        val bank1 = CogoBankInfo(
            bankId = UUID.fromString("15f62457-57c7-4a80-bc83-1535aa6ea021"),
            bankCode = "RBLP",
            bankName = "RBL BANK LTD",
            cogoAccountNo = "409001406475"
        )
        val bank2 = CogoBankInfo(
            bankId = UUID.fromString("b57f12ca-6288-4706-be17-6e52c8f8743c"),
            bankCode = "CITI",
            bankName = "Citibank N.A",
            cogoAccountNo = "0-021112-003"
        )
        val bank3 = CogoBankInfo(
            bankId = UUID.fromString("b57f12ca-6288-4706-be17-6e52c8f8743c"),
            bankCode = "INGU",
            bankName = "Citibank N.A",
            cogoAccountNo = "0-021112-003"
        )
        val bank4 = CogoBankInfo(
            bankId = UUID.fromString("c9218d66-b168-4752-a566-40ef4de7350f"),
            bankCode = "INGE",
            bankName = "Ing Bank N V",
            cogoAccountNo = "0670344095"
        )
        val bank5 = CogoBankInfo(
            bankId = UUID.fromString("22965db7-c92c-4fb4-9343-b5d2f0b69509"),
            bankCode = "RBLC",
            bankName = "RBL BANK LTD",
            cogoAccountNo = "409000876343"
        )
        val bank6 = CogoBankInfo(
            bankId = UUID.fromString("5d66d64a-9bde-4619-8fd4-bf38819f2a55"),
            bankCode = "RBLU",
            bankName = "RBL BANK LTD",
            cogoAccountNo = "409000824933"
        )
        val bank7 = CogoBankInfo(
            bankId = UUID.fromString("102ef08c-702a-41f7-b001-1c1df9714a5e"),
            bankCode = "RBLD",
            bankName = "RBL BANK LTD, LOWER PAREL",
            cogoAccountNo = "609000715480"
        )
        val bank8 = CogoBankInfo(
            bankId = UUID.fromString("0e19483d-15b2-4f8f-8b5e-3fe4b1c0dcf1"),
            bankCode = "INDUS",
            bankName = "INDUSIND BANK LTD, OPERA HOUSE",
            cogoAccountNo = "603014033080"
        )
        val bank9 = CogoBankInfo(
            bankId = UUID.fromString("0e19483d-15b2-4f8f-8b5e-3fe4b1c0dcf1"),
            bankCode = "INDUA",
            bankName = "INDUSIND BANK LTD, OPERA HOUSE",
            cogoAccountNo = "603014033080"
        )
        val bank10 = CogoBankInfo(
            bankId = UUID.fromString("c9218d66-b168-4752-a566-40ef4de7350f"),
            bankCode = "CCA",
            bankName = "Ing Bank N V",
            cogoAccountNo = "0670344095"
        )
        val bank11 = CogoBankInfo(
            bankId = UUID.fromString("b57f12ca-6288-4706-be17-6e52c8f8743c"),
            bankCode = "CITIS",
            bankName = "Citibank N.A.",
            cogoAccountNo = "0-021112-003"
        )
        val bank12 = CogoBankInfo(
            bankId = UUID.fromString("8300e176-16d4-4e99-9108-c293f49e8ebf"),
            bankCode = "RBLCP",
            bankName = "RBL BANK LTD, LOWER PAREL",
            cogoAccountNo = "609000842058"
        )
        val bankInformation: Map<String, CogoBankInfo> = mapOf(
            "RBLCP" to bank12, "CITIS" to bank11, "CCA" to bank10, "INDUA" to bank9,
            "INDUS" to bank8, "RBLD" to bank7, "RBLU" to bank6, "RBLC" to bank5, "INGE" to bank4, "INGU" to bank3, "CITI" to bank2, "RBLP" to bank1
        )
        return bankInformation.get(code)
    }
}
