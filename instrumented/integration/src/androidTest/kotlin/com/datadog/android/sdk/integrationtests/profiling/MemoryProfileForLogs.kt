package com.datadog.android.sdk.integrationtests.profiling

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.datadog.android.sdk.integrationtests.ActivityProfiling
import com.datadog.android.sdk.integrationtests.utils.MemoryProfilingRule
import com.datadog.android.sdk.integrationtests.utils.MockServerRule
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class MemoryProfileForLogs {

    @get:Rule
    val mockServerRule = MockServerRule(ActivityProfiling::class.java)
    @get:Rule
    val memoryProfilingRule = MemoryProfilingRule()

    @Test
    fun profileCrashLogWithLargeNumberOfAttributes() {
        val crash = IOException()
        val attributes = mutableMapOf<String, String>()
        for (i in 0..100) {
            attributes["key$i"] = "value$i"
        }

        memoryProfilingRule.profile({
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                repeat(50) {
                    mockServerRule.activity.logger.d(
                        "Test Crash",
                        crash,
                        attributes = attributes
                    )
                }
            }
        }, 400)
    }
}
