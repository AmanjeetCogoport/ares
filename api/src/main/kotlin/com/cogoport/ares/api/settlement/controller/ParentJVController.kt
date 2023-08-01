package com.cogoport.ares.api.settlement.controller

import com.cogoport.ares.api.settlement.entity.GlCode
import com.cogoport.ares.api.settlement.entity.GlCodeMaster
import com.cogoport.ares.api.settlement.entity.JournalCode
import com.cogoport.ares.api.settlement.entity.JvCategory
import com.cogoport.ares.api.settlement.service.interfaces.ParentJVService
import com.cogoport.ares.api.utils.Util
import com.cogoport.ares.common.models.Response
import com.cogoport.ares.model.common.ResponseList
import com.cogoport.ares.model.payment.AccMode
import com.cogoport.ares.model.settlement.ParentJournalVoucherResponse
import com.cogoport.ares.model.settlement.PostJVToSageRequest
import com.cogoport.ares.model.settlement.request.JvListRequest
import com.cogoport.ares.model.settlement.request.ParentJVUpdateRequest
import com.cogoport.ares.model.settlement.request.ParentJournalVoucherRequest
import com.cogoport.brahma.authentication.Auth
import com.cogoport.brahma.authentication.AuthResponse
import com.cogoport.brahma.hashids.Hashids
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.validation.Validated
import jakarta.inject.Inject
import java.util.UUID
import javax.validation.Valid

/**
 * Controller to handle all Journal.
 */

@Validated
@Controller("/parent-jv")
class ParentJVController {

    @Inject
    lateinit var parentJVService: ParentJVService

    @Inject
    lateinit var util: Util

    @Post
    suspend fun createJv(@Body request: ParentJournalVoucherRequest): Response<String?> {
        return Response<String?>().ok("Journal Voucher Created Successfully", parentJVService.createJournalVoucher(request))
    }

    @Auth
    @Get("/list{?jvListRequest*}")
    suspend fun getJournalVouchers(@Valid jvListRequest: JvListRequest, user: AuthResponse?, httpRequest: HttpRequest<*>): ResponseList<ParentJournalVoucherResponse> {
        jvListRequest.entityCode = util.getCogoEntityCode(user?.filters?.get("partner_id"))?.toInt() ?: jvListRequest.entityCode
        return Response<ResponseList<ParentJournalVoucherResponse>>().ok(parentJVService.getJournalVouchers(jvListRequest))
    }

    @Put("/update")
    suspend fun updateParentJv(@Valid @Body request: ParentJVUpdateRequest): String {
        return parentJVService.updateParentJv(request)
    }

    @Delete()
    suspend fun deleteJournalVoucherById(@QueryValue("id") id: String, @QueryValue("performedBy") performedBy: UUID): Response<String> {
        return Response<String>().ok("Journal Voucher deleted successfully", parentJVService.deleteJournalVoucherById(id, performedBy))
    }

    @Put
    suspend fun editJv(@Valid @Body request: ParentJournalVoucherRequest): Response<String> {
        return Response<String>().ok("Journal Voucher Edited Successfully", parentJVService.editJv(request))
    }

    @Post("/post-to-sage")
    suspend fun postJVToSage(@Valid @Body req: PostJVToSageRequest): Response<String> {
        return Response<String>().ok(
            HttpStatus.OK.name,
            if (parentJVService.postJVToSage(Hashids.decode(req.parentJvId)[0], req.performedBy)) "Success." else "Failed."
        )
    }

    @Get("/jv-category")
    suspend fun getJVCategories(@QueryValue("q") q: String?, @QueryValue("pageLimit") pageLimit: Int? = 10): List<JvCategory> {
        return parentJVService.getJvCategory(q, pageLimit)
    }

    @Get("/gl-code")
    suspend fun getGLCode(@QueryValue("entityCode") entityCode: Int?, @QueryValue("q") q: String?, @QueryValue("pageLimit") pageLimit: Int? = 10): List<GlCode> {
        return parentJVService.getGLCode(entityCode, q, pageLimit)
    }

    @Get("/gl-code-master")
    suspend fun getGLCodeMaster(@QueryValue("accMode") accMode: AccMode?, @QueryValue("q") q: String?, @QueryValue("pageLimit") pageLimit: Int? = 10, @QueryValue("entityCode") entityCode: Int? = 301): List<GlCodeMaster> {
        return parentJVService.getGLCodeMaster(accMode, q, pageLimit, entityCode)
    }

    @Get("/journal-code")
    suspend fun getJournalCode(@QueryValue("q") q: String?, @QueryValue("pageLimit") pageLimit: Int? = 10): List<JournalCode> {
        return parentJVService.getJournalCode(q, pageLimit)
    }

    @Get("/acc-mode")
    suspend fun getAccountMode(@QueryValue("q") q: String?, @QueryValue("glCode") glCode: String?): List<HashMap<String, String>> {
        return parentJVService.getAccountMode(q, glCode)
    }

    @Post("/bulk-post")
    suspend fun bulkPostingJvToSage() {
        return parentJVService.bulkPostingJvToSage()
    }
}
