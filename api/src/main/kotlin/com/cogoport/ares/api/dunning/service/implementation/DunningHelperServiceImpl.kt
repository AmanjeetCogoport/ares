package com.cogoport.ares.api.dunning.service.implementation

import com.cogoport.ares.api.dunning.DunningConstants
import com.cogoport.ares.api.dunning.model.SeverityEnum
import com.cogoport.ares.api.dunning.service.interfaces.DunningHelperService
import com.cogoport.ares.api.exception.AresError
import com.cogoport.ares.api.exception.AresException
import com.cogoport.ares.model.dunning.enum.AgeingBucketEnum
import com.cogoport.ares.model.dunning.enum.DunningExecutionFrequency
import com.cogoport.ares.model.dunning.request.DunningScheduleRule
import com.cogoport.ares.model.dunning.request.SendMailOfAllCommunicationToTradePartyReq
import com.cogoport.ares.model.payment.ServiceType
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Date

@Singleton
class DunningHelperServiceImpl : DunningHelperService {

    override suspend fun listSeverityLevelTemplates(): MutableMap<String, String> {
        val response: MutableMap<String, String> = mutableMapOf()

        SeverityEnum.values().forEach { severityLeve ->
            response[severityLeve.name] = severityLeve.severity
        }
        return response
    }

    override suspend fun calculateNextScheduleTime(scheduleRule: DunningScheduleRule): Date {
        val extractTime = extractHourAndMinute(scheduleRule.scheduleTime)
        val scheduleHour = extractTime["hour"]!!
        val scheduleMinute = extractTime["minute"]!!

        val scheduleTimeStampInGMT: Timestamp = when (DunningExecutionFrequency.valueOf(scheduleRule.dunningExecutionFrequency)) {
            DunningExecutionFrequency.ONE_TIME -> calculateNextScheduleTimeForOneTime(scheduleRule, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.DAILY -> calculateNextScheduleTimeForDaily(scheduleRule, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.WEEKLY -> calculateScheduleTimeForWeekly(scheduleRule.week!!, scheduleHour, scheduleMinute)
            DunningExecutionFrequency.MONTHLY -> calculateScheduleTimeForMonthly(scheduleRule.dayOfMonth!!, scheduleHour, scheduleMinute)
            else -> throw AresException(AresError.ERR_1002, "")
        }

        val actualTimestampInRespectiveTimeZone = scheduleTimeStampInGMT.time.minus(
            DunningConstants.TIME_ZONE_DIFFERENCE_FROM_GMT[DunningConstants.TimeZone.valueOf(scheduleRule.scheduleTimeZone)]
                ?: throw AresException(AresError.ERR_1002, "")
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val formattedDate = dateFormat.format(actualTimestampInRespectiveTimeZone)
        return dateFormat.parse(formattedDate)
    }

    private fun extractHourAndMinute(time: String): Map<String, String> {
        if (time.length != 5 ||
            (time.slice(0..1).toLong() > 24 || time.slice(0..1).toLong() < 0) ||
            (time.slice(3..4).toLong() > 60) || time.slice(3..4).toLong() < 0
        ) {
            throw AresException(AresError.ERR_1549, "")
        }

        return mapOf(
            "hour" to time.slice(0..1),
            "minute" to time.slice(3..4)
        )
    }

    private fun calculateNextScheduleTimeForOneTime(
        scheduleRule: DunningScheduleRule,
        scheduleHour: String,
        scheduleMinute: String
    ): Timestamp {
        val scheduleDateCal = Calendar.getInstance()

        scheduleDateCal.timeInMillis = System.currentTimeMillis()

        scheduleDateCal.set(Calendar.DAY_OF_MONTH, scheduleRule.oneTimeDate!!.slice(0..1).toInt())
        scheduleDateCal.set(Calendar.MONTH, (scheduleRule.oneTimeDate!!.slice(3..4).toInt()).minus(1))
        scheduleDateCal.set(Calendar.YEAR, scheduleRule.oneTimeDate!!.slice(6..9).toInt())
        scheduleDateCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        scheduleDateCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        val localTimestampWRTZone: Long = System.currentTimeMillis().plus(DunningConstants.EXTRA_TIME_TO_PROCESS_DATA_DUNNING)

        if (scheduleDateCal.timeInMillis < localTimestampWRTZone) {
            throw AresException(AresError.ERR_1551, "")
        }

        return Timestamp(scheduleDateCal.timeInMillis)
    }

    private fun calculateNextScheduleTimeForDaily(
        scheduleRule: DunningScheduleRule,
        scheduleHour: String,
        scheduleMinute: String
    ): Timestamp {
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = DunningConstants.TIME_ZONE_DIFFERENCE_FROM_GMT[DunningConstants.TimeZone.valueOf(scheduleRule.scheduleTimeZone)]?.plus(System.currentTimeMillis())!!

        if (todayCal.get(Calendar.HOUR_OF_DAY) > scheduleHour.toInt()) {
            todayCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        todayCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        todayCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        return Timestamp(todayCal.timeInMillis)
    }

    private fun calculateScheduleTimeForWeekly(week: DayOfWeek, scheduleHour: String, scheduleMinute: String): Timestamp {
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = System.currentTimeMillis()

        if (
            !(
                (todayCal.get(Calendar.DAY_OF_WEEK) == (week.ordinal + 1)) &&
                    (todayCal.get(Calendar.HOUR_OF_DAY) < scheduleHour.toInt())
                )
        ) {
            while (todayCal.get(Calendar.DAY_OF_WEEK) != (week.ordinal + 1)) {
                todayCal.add(Calendar.DAY_OF_WEEK, 1)
            }
        }

        todayCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        todayCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        return Timestamp(todayCal.timeInMillis)
    }

    private fun calculateScheduleTimeForMonthly(dayOfMonth: Int, scheduleHour: String, scheduleMinute: String): Timestamp {
        val todayCal = Calendar.getInstance()
        todayCal.timeInMillis = System.currentTimeMillis()

        if (todayCal.get(Calendar.DAY_OF_MONTH) > dayOfMonth ||
            (todayCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth && (todayCal.get(Calendar.HOUR_OF_DAY) > scheduleHour.toInt())) &&
            (todayCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth && (todayCal.get(Calendar.HOUR_OF_DAY) == scheduleHour.toInt() && todayCal.get(Calendar.MINUTE) > scheduleMinute.toInt()))
        ) {
            todayCal.add(Calendar.MONTH, 1)
        }

        todayCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        todayCal.set(Calendar.HOUR_OF_DAY, scheduleHour.toInt())
        todayCal.set(Calendar.MINUTE, scheduleMinute.toInt())

        return Timestamp(todayCal.timeInMillis)
    }

    override suspend fun getAgeingBucketDays(ageingBucketName: AgeingBucketEnum): IntArray {
        return when (ageingBucketName) {
            AgeingBucketEnum.ALL -> intArrayOf(0, 0)
            AgeingBucketEnum.AB_1_30 -> intArrayOf(0, 30)
            AgeingBucketEnum.AB_31_60 -> intArrayOf(31, 60)
            AgeingBucketEnum.AB_61_90 -> intArrayOf(61, 90)
            AgeingBucketEnum.AB_91_180 -> intArrayOf(91, 180)
            AgeingBucketEnum.AB_181_PLUS -> intArrayOf(181, 181)
            else -> throw AresException(AresError.ERR_1542, "")
        }
    }

    override suspend fun getServiceType(serviceType: ServiceType): List<ServiceType> {
        return when (serviceType) {
            ServiceType.FCL_FREIGHT -> listOf(
                ServiceType.FCL_FREIGHT, ServiceType.FCL_CUSTOMS, ServiceType.FCL_FREIGHT_LOCAL,
                ServiceType.FCL_CUSTOMS, ServiceType.FCL_CFS
            )

            ServiceType.LCL_FREIGHT -> listOf(
                ServiceType.LCL_FREIGHT, ServiceType.LCL_CUSTOMS, ServiceType.LCL_CUSTOMS_FREIGHT
            )

            ServiceType.LTL_FREIGHT -> listOf(
                ServiceType.LTL_FREIGHT
            )

            ServiceType.AIR_FREIGHT -> listOf(
                ServiceType.AIR_FREIGHT, ServiceType.AIR_CUSTOMS_FREIGHT, ServiceType.AIR_CUSTOMS,
                ServiceType.AIR_FREIGHT_LOCAL, ServiceType.DOMESTIC_AIR_FREIGHT
            )

            ServiceType.FTL_FREIGHT -> listOf(
                ServiceType.FTL_FREIGHT
            )

            ServiceType.HAULAGE_FREIGHT -> listOf(
                ServiceType.HAULAGE_FREIGHT
            )

            else -> throw AresException(AresError.ERR_1543, "")
        }
    }

    override suspend fun sendMailOfAllCommunicationToTradeParty(
        sendMailOfAllCommunicationToTradePartyReq: SendMailOfAllCommunicationToTradePartyReq,
        isSynchronousCall: Boolean
    ): String {

        if (isSynchronousCall) {
            return "We got your request. will sent you mail on ${sendMailOfAllCommunicationToTradePartyReq.userEmail} with report."
        }

        TODO("write code to send mail")
        return ""
    }
}
