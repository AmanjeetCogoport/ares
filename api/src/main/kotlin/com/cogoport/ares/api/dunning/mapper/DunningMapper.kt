package com.cogoport.ares.api.dunning.mapper

import com.cogoport.ares.model.dunning.request.CreateUserRequest
import com.cogoport.ares.model.dunning.request.OrgUserDetail
import org.mapstruct.Mapper
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface DunningMapper {

    fun convertUserRequestToUserInvitationRequest(request: CreateUserRequest): OrgUserDetail
}
