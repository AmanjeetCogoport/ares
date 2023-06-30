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
            SELECT * FROM organization_stakeholders WHERE organizationId IN (:organizationIds);
        """
    )
    suspend fun listOrganizationStakeholderUsingOrgId(organizationIds: List<UUID>): List<OrganizationStakeholder>

    @NewSpan
    @Query(
        """
                SELECT
                    DISTINCT ON (organization_stakeholder_id) organization_stakeholder_id,
                    organization_stakeholder_name
                FROM
                    organization_stakeholders
                WHERE
                    (:query IS NULL OR LOWER(organization_stakeholder_name) ILIKE :query)
                ORDER BY
                    organization_stakeholder_id
            """
    )
    suspend fun listDistinctlistOnorganizationStakeholders(
        query: String?
    ): List<CreditControllerResponse>

    @NewSpan
    @Query(
        """
           SELECT
                organization_id
            from
                organization_stakeholders
            WHERE
                organization_stakeholder_id :: UUID in (:organizationStakeholderIds)
        """
    )
    suspend fun listOrganizationIdBasedOnorganizationStakeholderIds(organizationStakeholderIds: List<UUID>?): List<UUID>

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
}
