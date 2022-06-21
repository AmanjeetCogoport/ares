package com.cogoport.ares.model.payment

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.Date
import java.util.UUID


@Introspected
@ReflectiveAccess
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude
data class AccountUtilizationResponse(
    @JsonProperty("id")
    var id: Long? = null,
    @JsonProperty("documentNo")
    var documentNo: Long? = null,
    @JsonProperty("documentValue")
    var documentValue: String? = null,
    @JsonProperty("zoneCode")
    var zoneCode: String? = null,
    @JsonProperty("serviceType")
    var serviceType: String? = null,
    @JsonProperty("documentStatus")
    var documentStatus: DocumentStatus? = null,
    @JsonProperty("entityCode")
    var entityCode: Int? = null,
    @JsonProperty("category")
    var category: String? = null,
    @JsonProperty("orgSerialId")
    var orgSerialId: Long? = null,
    @JsonProperty("sageOrganizationId")
    var sageOrganizationId: String? = null,
    @JsonProperty("organizationId")
    var organizationId: UUID? = null,
    @JsonProperty("organizationName")
    var organizationName: String? = null,
    @JsonProperty("accCode")
    var accCode: Int? = null,
    @JsonProperty("accType")
    var accType: AccountType? = null,
    @JsonProperty("accMode")
    var accMode: AccMode? = null,
    @JsonProperty("signFlag")
    var signFlag: Short = 0,
    @JsonProperty("currency")
    var currency: String = "INR",
    @JsonProperty("ledCurrency")
    val ledCurrency: String = "INR",
    @JsonProperty("amountCurr")
    var amountCurr: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("amountLoc")
    var amountLoc: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("payCurr")
    var payCurr: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("payLoc")
    var payLoc: BigDecimal? = 0.toBigDecimal(),
    @JsonProperty("dueDate")
    var dueDate: Date? = null,
    @JsonProperty("transactionDate")
    var transactionDate: Date? = null,
    @JsonProperty("createdAt")
    var createdAt: Timestamp? = Timestamp.from(Instant.now()),
    @JsonProperty("updatedAt")
    var updatedAt: Timestamp? = Timestamp.from(Instant.now()),
)
