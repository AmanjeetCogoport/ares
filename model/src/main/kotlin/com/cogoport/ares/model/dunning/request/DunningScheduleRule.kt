package com.cogoport.ares.model.dunning.request

import com.cogoport.ares.model.dunning.enum.DunningExecutionFrequency
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.micronaut.core.annotation.Introspected
import java.sql.Time
import java.time.DayOfWeek

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DunningScheduleRule(
    var scheduleTime: Time,
    var scheduleTimeZone: String,
    var dunningExecutionFrequency: DunningExecutionFrequency?,
    var week: List<DayOfWeek>?,
    var month: Int?,
)
