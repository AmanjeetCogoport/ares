package com.cogoport.ares.model.dunning.request

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import java.time.DayOfWeek

@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class DunningScheduleRule(
    @JsonProperty("scheduleTime")
    var scheduleTime: String,
    @JsonProperty("scheduleTimeZone")
    var scheduleTimeZone: String,
    @JsonProperty("dunningExecutionFrequency")
    var dunningExecutionFrequency: String,
    @JsonProperty("week")
    var week: DayOfWeek?,
    @JsonProperty("dayOfMonth")
    var dayOfMonth: Int?,
    @JsonProperty("oneTimeDate")
    var oneTimeDate: String?
)
