package com.cogoport.ares.model.common

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import kotlin.collections.HashMap

@Introspected
data class CommunicationRequest(
    @JsonProperty("variables")
    var variables: HashMap<String, Any?>,
    @JsonProperty("reply_to_message_id")
    var replyToMessageId: String?,
    @JsonProperty("organization_id")
    var organizationId: String?,
    @JsonProperty("notify_on_bounce")
    var notifyOnBounce: Boolean?,
    @JsonProperty("service_id")
    var serviceId: String?,
    @JsonProperty("service")
    var service: String?,
    @JsonProperty("type")
    var type: String?,
    @JsonProperty("sender")
    var sender: String?,
    @JsonProperty("recipient")
    var recipient: String?,
    @JsonProperty("cc_emails")
    var ccMails: MutableList<String>? = mutableListOf<String>(),
    @JsonProperty("template_name")
    var templateName: String?
)
