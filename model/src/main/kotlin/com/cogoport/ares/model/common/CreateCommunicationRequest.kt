package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.data.annotation.MappedEntity
import java.util.UUID
import kotlin.collections.HashMap

@MappedEntity
data class CreateCommunicationRequest(
    @JsonProperty("template_name")
    var templateName: String?,
    @JsonProperty("performed_by_user_id")
    var performedByUserId: UUID? = null,
    @JsonProperty("performed_by_user_name")
    var performedByUserName: String? = null,
    @JsonProperty("recipient_email")
    var recipientEmail: String?,
    @JsonProperty("sender_email")
    var senderEmail: String?,
    @JsonProperty("cc_emails")
    var ccEmails: List<String?>? = mutableListOf<String?>(),
    @JsonProperty("email_variables")
    var emailVariables: HashMap<String, String?>?
)
