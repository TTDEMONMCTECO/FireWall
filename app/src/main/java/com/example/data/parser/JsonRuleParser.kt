package com.example.data.parser

import com.example.domain.model.AccessType
import com.example.domain.model.AppRuleConfig
import com.example.domain.model.RuleFilter
import org.json.JSONArray
import org.json.JSONObject

data class FirewallJsonData(
    val apps: List<AppRuleConfig>,
    val filters: List<RuleFilter>
)

object JsonRuleParser {

    fun parseJsonString(jsonString: String): FirewallJsonData {
        val root = JSONObject(jsonString)
        
        val appsList = mutableListOf<AppRuleConfig>()
        if (root.has("apps")) {
            val appsArray = root.getJSONArray("apps")
            for (i in 0 until appsArray.length()) {
                val appObj = appsArray.getJSONObject(i)
                val appName = appObj.optString("appName", "Unknown")
                val pkgName = appObj.optString("pkgName", "")
                if (pkgName.isNotEmpty()) {
                    appsList.add(
                        AppRuleConfig(
                            pkgName = pkgName,
                            appName = appName,
                            wifi = AccessType.NONE,
                            mobile = AccessType.NONE,
                            isSystemApp = pkgName.startsWith("com.android") || pkgName.contains("system")
                        )
                    )
                }
            }
        }

        val filtersList = mutableListOf<RuleFilter>()
        if (root.has("filters")) {
            val filtersArray = root.getJSONArray("filters")
            for (i in 0 until filtersArray.length()) {
                val filterObj = filtersArray.getJSONObject(i)
                val appName = filterObj.optString("appName", "App")
                val isCustom = filterObj.optBoolean("isCustom", true)
                val mobileStr = filterObj.optString("mobile", "none")
                val wifiStr = filterObj.optString("wifi", "none")
                val pkg1Name = filterObj.optString("pkg1Name", "")
                val pkg2Name = if (filterObj.has("pkg2Name") && !filterObj.isNull("pkg2Name")) {
                    filterObj.getString("pkg2Name")
                } else null
                val port = filterObj.optInt("port", -1)
                val priority = filterObj.optInt("priority", 0)
                val proto = filterObj.optString("proto", "tcp")
                val server = filterObj.optString("server", "*")
                val serverStrType = filterObj.optString("serverStrType", "ip4")

                if (pkg1Name.isNotEmpty()) {
                    filtersList.add(
                        RuleFilter(
                            appName = appName,
                            pkg1Name = pkg1Name,
                            pkg2Name = pkg2Name,
                            wifi = AccessType.fromString(wifiStr),
                            mobile = AccessType.fromString(mobileStr),
                            port = port,
                            priority = priority,
                            proto = proto,
                            server = server,
                            serverStrType = serverStrType,
                            isCustom = isCustom
                        )
                    )
                }
            }
        }

        return FirewallJsonData(apps = appsList, filters = filtersList)
    }

    fun toJsonString(data: FirewallJsonData): String {
        val root = JSONObject()

        val appsArray = JSONArray()
        for (app in data.apps) {
            val appObj = JSONObject()
            appObj.put("appName", app.appName)
            appObj.put("pkgName", app.pkgName)
            appsArray.put(appObj)
        }
        root.put("apps", appsArray)

        val filtersArray = JSONArray()
        for (filter in data.filters) {
            val filterObj = JSONObject()
            filterObj.put("appName", filter.appName)
            filterObj.put("isCustom", filter.isCustom)
            filterObj.put("mobile", filter.mobile.name.lowercase())
            filterObj.put("wifi", filter.wifi.name.lowercase())
            filterObj.put("pkg1Name", filter.pkg1Name)
            if (filter.pkg2Name != null) {
                filterObj.put("pkg2Name", filter.pkg2Name)
            }
            filterObj.put("port", filter.port)
            filterObj.put("priority", filter.priority)
            filterObj.put("proto", filter.proto)
            filterObj.put("server", filter.server)
            filterObj.put("serverStrType", filter.serverStrType)
            filtersArray.put(filterObj)
        }
        root.put("filters", filtersArray)

        return root.toString(2)
    }
}
