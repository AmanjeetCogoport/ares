package com.cogoport.ares.api.settlement.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import kotlin.collections.HashMap
import java.util.UUID


@Introspected

data class SettlementNotificationRequest(
    @JsonProperty("template_name")
    var templateName: String?,
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
    var emailVariables: HashMap<String?, String?>,
    @JsonProperty("notification_variables")
    var notificationVariables: HashMap<String?, String?>
)
