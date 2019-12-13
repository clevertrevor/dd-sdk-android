package com.datadog.android.sdk.integrationtests.utils.assertj

import com.google.gson.JsonObject
import org.assertj.core.api.AbstractAssert

class LogsListAssert(actual: List<JsonObject>) : AbstractAssert<LogsListAssert, List<JsonObject>>
    (actual, LogsListAssert::class.java) {

    fun containsOnlyLogsWithMessagesInOrder(vararg messages: String): LogsListAssert {
        if (messages.size != actual.size) failWithMessage(
            "Expected to find ${messages.size} logs, but was ${actual.size}"
        )

        actual.forEachIndexed { index, log ->
            if (messages[index] != log.get(MESSAGE_KEY).asString) {
                failWithMessage(
                    "Message '${messages[index]}' could not be found in log [ $log ]"
                )
            }
        }

        return this
    }

    fun containsLogsWithMessagesInOrder(vararg messages: String): LogsListAssert {
        if (messages.size > actual.size) failWithMessage(
            "We had less messages than the ones we " +
                    "were expecting "
        )
        var index = 0
        actual.forEach {
            if (messages[index] == it.get(MESSAGE_KEY).asString) {
                index++
            }
            if (index >= messages.size) return this // we are ok
        }

        if (index < messages.size) {
            failWithMessage(
                "We were expecting following messages: [${messages.joinToString { "," }}] but " +
                        "they could not be found"
            )
        }
        return this
    }

    fun hasService(name: String): LogsListAssert {
        actual.forEach {
            if (it.get(SERVICE_NAME_KEY).asString != name) {
                failWithMessage(
                    "We were expecting '$name' as the service name for " +
                            "the log [ $it ]"
                )
            }
        }

        return this
    }

    fun hasAttributes(vararg attributes: Pair<String, String>): LogsListAssert {
        actual.forEach { log ->
            attributes.forEach {
                if (log.get(it.first)?.asString != it.second) {
                    failWithMessage(
                        "We were expecting '$it' attribute for " +
                                "the log [ $log ]"
                    )
                }
            }
        }

        return this
    }

    fun hasTags(vararg tags: String): LogsListAssert {
        val parsedTags = tags
            .joinToString(",")
        actual.forEach { log ->
            if (log.get(TAGS_KEY)?.asString != parsedTags) {
                failWithMessage(
                    "We were expecting '$parsedTags' as tags for " +
                            "the log [ $log ]"
                )
            }
        }

        return this
    }

    companion object {
        internal fun assertThat(actual: List<JsonObject>): LogsListAssert =
            LogsListAssert(actual)

        const val SERVICE_NAME_KEY = "service"
        const val MESSAGE_KEY = "message"
        const val TAGS_KEY = "ddtags"
    }
}