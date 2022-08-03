package com.cogoport.ares.api.settlement.mapper

import com.cogoport.ares.api.settlement.entity.JournalVoucher
import org.mapstruct.Mapper

@Mapper
interface JournalVoucherMapper {

    fun convertToModel(journalVoucher: JournalVoucher): com.cogoport.ares.model.settlement.JournalVoucher

}