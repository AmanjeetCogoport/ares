package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.OrganizationStakeholder
import com.cogoport.ares.model.dunning.response.CreditControllerResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface OrganizationStakeholderRepo : CoroutineCrudRepository<OrganizationStakeholder, Long> {

    @NewSpan
    @Query(
        """
                SELECT
                    DISTINCT ON (organization_stakeholder_id) organization_stakeholder_id,
                    organization_stakeholder_name
                FROM
                    organization_stakeholders
                WHERE
                    is_active = true
                    AND organization_stakeholder_type::VARCHAR = :stakeHolderType
                    AND (:query IS NULL OR LOWER(organization_stakeholder_name) ILIKE :query)
                    OFFSET 
                    GREATEST(0, ((:pageIndex - 1) * :pageSize))
                LIMIT 
                    :pageSize
            """
    )
    suspend fun listDistinctOrganizationStakeholders(
        query: String?,
        stakeHolderType: String,
        pageIndex: Int? = 1,
        pageSize: Long? = 100
    ): List<CreditControllerResponse>

    @NewSpan
    @Query(
        """
           SELECT
                DISTINCT(organization_id)
            FROM
                organization_stakeholders
            WHERE
                organization_stakeholder_id :: UUID in (:organizationStakeholderIds)
                AND is_active = true
                AND organization_stakeholder_type::VARCHAR = :stakeHolderType
        """
    )
    suspend fun getOrgsByStakeHolders(organizationStakeholderIds: List<UUID>?, stakeHolderType: String): List<UUID>

    @NewSpan
    @Query(
        """
            SELECT
                *
            FROM
                organization_stakeholders
            WHERE
                organization_id::UUID = :organizationId
                AND :organizationStakeholderType is null or organization_stakeholder_type::varchar = :organizationStakeholderType
            LIMIT 1
        """
    )
    suspend fun getOrganizationStakeholdersUsingOrgId(
        organizationId: UUID,
        organizationStakeholderType: String?
    ): OrganizationStakeholder?

    @NewSpan
    @Query(
        """
        SELECT COALESCE((ARRAY_AGG(organization_segment))[1], null) FROM organization_stakeholders WHERE organization_id::UUID = :organizationId::UUID AND is_active = true
    """
    )
    suspend fun getOrgSegment(organizationId: UUID): String?

    @NewSpan
    @Query(
        """
            SELECT
                *
            FROM
                organization_stakeholders
            WHERE
                organization_id::UUID = :organizationId
                AND :organizationStakeholderType is null or organization_stakeholder_type::varchar = :organizationStakeholderType
            """
    )
    suspend fun getOrganisationStakeholdersList(
        organizationId: UUID,
        organizationStakeholderType: String?
    ): List<OrganizationStakeholder>
}
