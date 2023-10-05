package com.cogoport.ares.api.settlement.service.implementation

import com.cogoport.ares.api.common.models.ExchangeRequest
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.api.gateway.ExchangeClient
import com.cogoport.ares.api.utils.logger
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.payment.AccountType
import com.cogoport.ares.model.payment.DocStatus
import com.cogoport.ares.model.payment.InvoiceType
import com.cogoport.ares.model.settlement.SettlementType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.math.BigDecimal

@Singleton
open class SettlementServiceHelper {

    @Inject
    lateinit var exchangeClient: ExchangeClient

    fun getDocumentType(accType: AccountType, signFlag: Short, accMode: AccMode): String {
        return when (accType) {
            AccountType.SINV -> InvoiceType.SINV.value
            AccountType.SCN -> InvoiceType.SCN.value
            AccountType.SREIMBCN -> InvoiceType.SREIMBCN.value
            AccountType.SDN -> InvoiceType.SDN.value
            AccountType.REC -> InvoiceType.REC.value
            AccountType.PINV -> InvoiceType.PINV.value
            AccountType.PCN -> InvoiceType.PCN.value
            AccountType.PDN -> InvoiceType.PDN.value
            AccountType.PAY -> InvoiceType.PAY.value
            AccountType.ROFF -> getVoucherType(signFlag) + InvoiceType.ROFF.value
            AccountType.WOFF -> getVoucherType(signFlag) + InvoiceType.WOFF.value
            AccountType.EXCH -> getVoucherType(signFlag) + InvoiceType.EXCH.value
            AccountType.OUTST -> getVoucherType(signFlag) + InvoiceType.OUTST.value
            AccountType.JVNOS -> getVoucherType(signFlag) + InvoiceType.JVNOS.value
            AccountType.SREIMB -> InvoiceType.SREIMB.value
            AccountType.PREIMB -> InvoiceType.PREIMB.value
            AccountType.ICJV -> getVoucherType(signFlag) + InvoiceType.ICJV.value
            AccountType.DBTRC -> getVoucherType(signFlag) + InvoiceType.DBTRC.value
            AccountType.STMNT -> getVoucherType(signFlag) + InvoiceType.STMNT.value
            AccountType.RECPT -> getVoucherType(signFlag) + InvoiceType.RECPT.value
            AccountType.FAR12 -> getVoucherType(signFlag) + InvoiceType.FAR12.value
            AccountType.CSTRV -> getVoucherType(signFlag) + InvoiceType.CSTRV.value
            AccountType.NEWPR -> getVoucherType(signFlag) + InvoiceType.NEWPR.value
            AccountType.SPDIR -> getVoucherType(signFlag) + InvoiceType.SPDIR.value
            AccountType.CLOSE -> getVoucherType(signFlag) + InvoiceType.CLOSE.value
            AccountType.STOCK -> getVoucherType(signFlag) + InvoiceType.STOCK.value
            AccountType.GENAJ -> getVoucherType(signFlag) + InvoiceType.GENAJ.value
            AccountType.PREPY -> getVoucherType(signFlag) + InvoiceType.PREPY.value
            AccountType.FAIS3 -> getVoucherType(signFlag) + InvoiceType.FAIS3.value
            AccountType.FAAR2 -> getVoucherType(signFlag) + InvoiceType.FAAR2.value
            AccountType.MFIRC -> getVoucherType(signFlag) + InvoiceType.MFIRC.value
            AccountType.CURVR -> getVoucherType(signFlag) + InvoiceType.CURVR.value
            AccountType.INTER -> getVoucherType(signFlag) + InvoiceType.INTER.value
            AccountType.FAS12 -> getVoucherType(signFlag) + InvoiceType.FAS12.value
            AccountType.PAYRL -> getVoucherType(signFlag) + InvoiceType.PAYRL.value
            AccountType.GENSM -> getVoucherType(signFlag) + InvoiceType.GENSM.value
            AccountType.FAIR3 -> getVoucherType(signFlag) + InvoiceType.FAIR3.value
            AccountType.WIPCS -> getVoucherType(signFlag) + InvoiceType.WIPCS.value
            AccountType.MFIIS -> getVoucherType(signFlag) + InvoiceType.MFIIS.value
            AccountType.BANK -> getVoucherType(signFlag) + InvoiceType.BANK.value
            AccountType.MSCOP -> getVoucherType(signFlag) + InvoiceType.MSCOP.value
            AccountType.RECJL -> getVoucherType(signFlag) + InvoiceType.RECJL.value
            AccountType.MFIOP -> getVoucherType(signFlag) + InvoiceType.MFIOP.value
            AccountType.EXPNS -> getVoucherType(signFlag) + InvoiceType.EXPNS.value
            AccountType.CSDIR -> getVoucherType(signFlag) + InvoiceType.CSDIR.value
            AccountType.CLOS1 -> getVoucherType(signFlag) + InvoiceType.CLOS1.value
            AccountType.GEN -> getVoucherType(signFlag) + InvoiceType.GEN.value
            AccountType.MISC -> getVoucherType(signFlag) + InvoiceType.MISC.value
            AccountType.OPDIV -> getVoucherType(signFlag) + InvoiceType.OPDIV.value
            AccountType.CONTR -> getVoucherType(signFlag) + InvoiceType.CONTR.value
            AccountType.BANK -> getVoucherType(signFlag) + InvoiceType.BANK.value
            AccountType.CRRCV -> getVoucherType(signFlag) + InvoiceType.CRRCV.value
            AccountType.CSINV -> getVoucherType(signFlag) + InvoiceType.CSINV.value
            AccountType.ZSINV -> getVoucherType(signFlag) + InvoiceType.ZSINV.value
            AccountType.SPINV -> getVoucherType(signFlag) + InvoiceType.SPINV.value
            AccountType.CSMEM -> getVoucherType(signFlag) + InvoiceType.CSMEM.value
            AccountType.ZSMEM -> getVoucherType(signFlag) + InvoiceType.ZSMEM.value
            AccountType.SPMEM -> getVoucherType(signFlag) + InvoiceType.SPMEM.value
            AccountType.RECJV -> getVoucherType(signFlag) + InvoiceType.RECJV.value
            AccountType.PAYJV -> getVoucherType(signFlag) + InvoiceType.PAYJV.value
            AccountType.CTDS -> getVoucherType(signFlag) + InvoiceType.CTDS.value
            AccountType.VTDS -> getVoucherType(signFlag) + InvoiceType.VTDS.value
            AccountType.SREIMBCN -> getVoucherType(signFlag) + InvoiceType.SREIMBCN.value
            AccountType.EXP -> getVoucherType(signFlag) + InvoiceType.EXP.value
            else -> throw AresException(AresError.ERR_1009, "accountType $accType")
        }
    }

    private fun getVoucherType(signFlag: Short): String {
        return when (signFlag.toInt()) {
            1 -> "Debit "
            -1 -> "Credit "
            else -> { throw AresException(AresError.ERR_1009, "signFlag") }
        }
    }

    fun getDocumentStatus(docAmount: BigDecimal, balanceAmount: BigDecimal, docType: SettlementType): String {
        val payments = listOf(SettlementType.REC, SettlementType.PAY, SettlementType.SCN, SettlementType.PCN)
        return if (balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
            if (payments.contains(docType)) DocStatus.UTILIZED.value else DocStatus.PAID.value
        } else if (docAmount.compareTo(balanceAmount) != 0) {
            if (payments.contains(docType)) DocStatus.PARTIAL_UTILIZED.value else DocStatus.PARTIAL_PAID.value
        } else if (docAmount.compareTo(balanceAmount) == 0) {
            if (payments.contains(docType)) DocStatus.UNUTILIZED.value else DocStatus.UNPAID.value
        } else {
            throw AresException(AresError.ERR_1504, "")
        }
    }

    /**
     * Get Exchange Rate from AWS Lambda.
     * @param: from
     * @param: to
     * @param: transactionDate
     * @return: BgDecimal
     */
    suspend fun getExchangeRate(from: String, to: String, transactionDate: String): BigDecimal {
        try {
            return exchangeClient.getExchangeRate(ExchangeRequest(from, to, transactionDate)).exchangeRate
        } catch (e: Exception) {
            logger().error("Exchange Rate not found in for {} to {} for date: {}", from, to, transactionDate)
            throw AresException(AresError.ERR_1505, "$from to $to")
        }
    }

    fun <T : Any> getJvList(classType: Class<T>): List<T> {
        return if (classType == SettlementType::class.java) {
            listOf(
                SettlementType.WOFF,
                SettlementType.ROFF,
                SettlementType.EXCH,
                SettlementType.JVNOS,
                SettlementType.OUTST,
                SettlementType.ICJV,
                SettlementType.GENSM,
                SettlementType.ZSMEP,
                SettlementType.PAYMT,
                SettlementType.FAIS3,
                SettlementType.SPDIR,
                SettlementType.GEN,
                SettlementType.NEWPR,
                SettlementType.PAYRL,
                SettlementType.BANK,
                SettlementType.CONTR,
                SettlementType.STOCK,
                SettlementType.MTC,
                SettlementType.WIPCO,
                SettlementType.RECP,
                SettlementType.MSCOP,
                SettlementType.INTER,
                SettlementType.FAS12,
                SettlementType.RECJL,
                SettlementType.OPDIV,
                SettlementType.CLOS1,
                SettlementType.ZDN,
                SettlementType.UNRIN,
                SettlementType.MFIRC,
                SettlementType.PREPY,
                SettlementType.FIXDP,
                SettlementType.FAIR3,
                SettlementType.DBTRC,
                SettlementType.WIPCS,
                SettlementType.MFIOP,
                SettlementType.CSTRV,
                SettlementType.RECPT,
                SettlementType.CRRCV,
                SettlementType.CLOSE,
                SettlementType.ZSMFR,
                SettlementType.SAINV,
                SettlementType.RPI,
                SettlementType.EXPNS,
                SettlementType.MFIIS,
                SettlementType.FAR12,
                SettlementType.CURVR,
                SettlementType.GENAJ,
                SettlementType.ZSDN,
                SettlementType.CSDIR,
                SettlementType.MTCCV,
                SettlementType.MISC,
                SettlementType.STMNT,
                SettlementType.FAAR2,
                SettlementType.CSINV,
                SettlementType.ZSINV,
                SettlementType.SPINV,
                SettlementType.CSMEM,
                SettlementType.SPMEM,
                SettlementType.ZSMEM,
                SettlementType.PAYJV,
                SettlementType.RECJV,
                SettlementType.CTDS,
                SettlementType.VTDS
            ) as List<T>
        } else {
            listOf(
                AccountType.WOFF,
                AccountType.ROFF,
                AccountType.EXCH,
                AccountType.JVNOS,
                AccountType.OUTST,
                AccountType.ICJV,
                AccountType.GENSM,
                AccountType.ZSMEP,
                AccountType.PAYMT,
                AccountType.FAIS3,
                AccountType.SPDIR,
                AccountType.GEN,
                AccountType.NEWPR,
                AccountType.PAYRL,
                AccountType.BANK,
                AccountType.CONTR,
                AccountType.STOCK,
                AccountType.MTC,
                AccountType.WIPCO,
                AccountType.RECP,
                AccountType.MSCOP,
                AccountType.INTER,
                AccountType.REC,
                AccountType.FAS12,
                AccountType.RECJL,
                AccountType.OPDIV,
                AccountType.CLOS1,
                AccountType.ZDN,
                AccountType.UNRIN,
                AccountType.MFIRC,
                AccountType.PREPY,
                AccountType.FIXDP,
                AccountType.FAIR3,
                AccountType.DBTRC,
                AccountType.WIPCS,
                AccountType.MFIOP,
                AccountType.CSTRV,
                AccountType.RECPT,
                AccountType.CRRCV,
                AccountType.CLOSE,
                AccountType.ZSMFR,
                AccountType.SAINV,
                AccountType.ZSMEM,
                AccountType.RPI,
                AccountType.EXPNS,
                AccountType.MFIIS,
                AccountType.FAR12,
                AccountType.CURVR,
                AccountType.GENAJ,
                AccountType.ZSDN,
                AccountType.CSDIR,
                AccountType.MTCCV,
                AccountType.MISC,
                AccountType.STMNT,
                AccountType.CSINV,
                AccountType.ZSINV,
                AccountType.SPINV,
                AccountType.CSMEM,
                AccountType.SPMEM,
                AccountType.ZSMEM,
                AccountType.PAYJV,
                AccountType.RECJV,
                AccountType.VTDS,
                AccountType.CTDS,
            ) as List<T>
        }
    }
}
