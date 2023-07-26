package com.cogoport.ares.api.dunning.entity

import com.cogoport.ares.api.common.enums.TokenObjectTypes
import com.cogoport.ares.api.common.enums.TokenTypes
import com.cogoport.ares.model.dunning.model.TokenData
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import java.sql.Timestamp
import javax.persistence.GeneratedValue

@MappedEntity(value = "tokens")
data class AresTokens(
    @field:Id @GeneratedValue
    var id: Long?,
    var objectId: Long,
    var objectType: TokenObjectTypes,
    var tokenType: TokenTypes,
    var token: String,
    @MappedProperty(type = DataType.JSON)
    var data: TokenData?,
    var expiryTime: Timestamp?,
    @DateCreated var createdAt: Timestamp?,
    @DateUpdated var updatedAt: Timestamp?
)
