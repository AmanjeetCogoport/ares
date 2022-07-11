package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.SettledInvoice
import org.mapstruct.Mapper

@Mapper
interface SettledInvoiceMapper {
    fun convertToModel(historyDocument: SettledInvoice?): com.cogoport.ares.model.settlement.SettledInvoice
}
