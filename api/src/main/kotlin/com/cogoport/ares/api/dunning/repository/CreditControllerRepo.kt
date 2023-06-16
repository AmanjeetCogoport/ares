package com.cogoport.ares.api.dunning.repository

import com.cogoport.ares.api.dunning.entity.CreditController
import com.cogoport.ares.model.dunning.response.CreditControllerResponse
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import io.micronaut.tracing.annotation.NewSpan
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface CreditControllerRepo : CoroutineCrudRepository<CreditController, Long> {

    @NewSpan
    @Query(
        """
            SELECT * FROM credit_controllers WHERE organizationId IN (:organizationIds);
        """
    )
    suspend fun listCreditControllersUsingOrgId(organizationIds: List<UUID>): List<CreditController>

    @NewSpan
    @Query(
        """
                SELECT
                    DISTINCT ON (credit_controller_id) credit_controller_id,
                    credit_controller_name
                FROM
                    credit_controllers
                WHERE
                    (:query IS NULL OR LOWER(credit_controller_name) ILIKE :query)
                ORDER BY
                    credit_controller_id
            """
    )
    suspend fun listDistinctCreditControllers(
        query: String?
    ): List<CreditControllerResponse>

    @NewSpan
    @Query(
        """
           SELECT
                organization_id
            from
                credit_controllers
            WHERE
                credit_controller_id :: UUID in (:creditControllerIds)
        """
    )
    suspend fun listOrganizationIdBasedOnCreditControllers(creditControllerIds: List<UUID>?): List<UUID>
}
