package com.cogoport.ares.api.settlement.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.sql.Timestamp
import java.util.UUID

@MappedEntity(value = "incident_mappings")
data class IncidentMappings(
    @field:Id @GeneratedValue
    val id: Long?,
    @MappedProperty(type = DataType.JSON)
    val accountUtilizationIds: Any?,
    @MappedProperty(type = DataType.JSON)
    val data: Any?,
    val incidentType: String?,
    val incidentStatus: String?,
    val organizationName: String?,
    val entityCode: Int?,
    val createdBy: UUID?,
    val updatedBy: UUID?,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?
)
