package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID
import kotlin.collections.HashMap

@Introspected
@MappedEntity
data class CreateCommunicationRequest(
    @JsonProperty("template_name")
    var emailTemplateName: String?,
    @JsonProperty("notification_template_name")
    var notificationTemplateName: String?,
    @JsonProperty("performed_by_user_id")
    var performedByUserId: UUID?,
    @JsonProperty("performed_by_user_name")
    var performedByUserName: String?,
    @JsonProperty("performed_by_user_type")
    var performedByUserType: String?,
    @JsonProperty("recipient_email")
    var recipientEmail: String?,
    @JsonProperty("sender_email")
    var senderEmail: String?,
    @JsonProperty("cc_emails")
    var ccEmails: List<String?>? = mutableListOf<String?>(),
    @JsonProperty("email_variables")
    var emailVariables: HashMap<String?, String?>?,
    @JsonProperty("notification_variables")
    var notificationVariables: HashMap<String?, String?>?
)
