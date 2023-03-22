package com.cogoport.ares.api.common

import org.json.JSONObject
import org.json.XML

class SageStatus {

    companion object {

        fun getZstatus(processedResponse: JSONObject?): String? {
            val content = processedResponse?.getJSONObject("soapenv:Envelope")
                ?.getJSONObject("soapenv:Body")
                ?.getJSONObject("wss:runResponse")
                ?.getJSONObject("runReturn")
                ?.getJSONObject("resultXml")
                ?.get("content")

            val response = XML.toJSONObject(content.toString())
            val status = response?.getJSONObject("RESULT")
                ?.getJSONObject("GRP")
                ?.getJSONArray("FLD")
                ?.getJSONObject(1)
                ?.get("content")

            return status.toString()
        }
    }
}
