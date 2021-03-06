/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.android.log.internal.logger

import com.datadog.android.log.forge.Configurator
import com.datadog.tools.unit.annotations.SystemErrorStream
import com.datadog.tools.unit.annotations.SystemOutStream
import com.datadog.tools.unit.extensions.SystemOutputExtension
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(SystemOutputExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class LogcatLogHandlerTest {

    lateinit var testedHandler: LogHandler

    lateinit var fakeServiceName: String
    lateinit var fakeLoggerName: String
    lateinit var fakeMessage: String
    lateinit var fakeTags: Set<String>
    lateinit var fakeAttributes: Map<String, Any?>

    var fakeLevel: Int = 0

    @Forgery
    lateinit var fakeThrowable: Throwable

    @BeforeEach
    fun `set up`(forge: Forge) {
        fakeServiceName = forge.anAlphabeticalString()
        fakeLoggerName = forge.anAlphabeticalString()
        fakeMessage = forge.anAlphabeticalString()
        fakeLevel = forge.anInt(2, 8)
        fakeAttributes = forge.aMap { anAlphabeticalString() to anInt() }
        fakeTags = forge.aList { anAlphabeticalString() }.toSet()

        testedHandler = LogcatLogHandler(fakeServiceName)
    }

    @Test
    fun `outputs log`(
        @SystemOutStream outputStream: ByteArrayOutputStream,
        @SystemErrorStream errorStream: ByteArrayOutputStream
    ) {
        testedHandler.handleLog(
            fakeLevel,
            fakeMessage,
            fakeThrowable,
            fakeAttributes,
            fakeTags
        )

        val prefix = levels[fakeLevel]
        assertThat(outputStream.toString())
            .isEqualTo("$prefix/$fakeServiceName: $fakeMessage\n")
        assertThat(errorStream.toString())
            .startsWith("${fakeThrowable.javaClass.canonicalName}: ${fakeThrowable.message}")
    }

    @Test
    fun `outputs log on background thread`(
        forge: Forge,
        @SystemOutStream outputStream: ByteArrayOutputStream,
        @SystemErrorStream errorStream: ByteArrayOutputStream
    ) {
        val threadName = forge.anAlphabeticalString()
        val countDownLatch = CountDownLatch(1)
        val thread = Thread({
            testedHandler.handleLog(
                fakeLevel,
                fakeMessage,
                fakeThrowable,
                fakeAttributes,
                fakeTags
            )
            countDownLatch.countDown()
        }, threadName)

        thread.start()
        countDownLatch.await(1, TimeUnit.SECONDS)

        val prefix = levels[fakeLevel]
        assertThat(outputStream.toString())
            .isEqualTo("$prefix/$fakeServiceName: $fakeMessage\n")
        assertThat(errorStream.toString())
            .startsWith("${fakeThrowable.javaClass.canonicalName}: ${fakeThrowable.message}")
    }

    @Test
    fun `outputs minimal log`(
        @SystemOutStream outputStream: ByteArrayOutputStream,
        @SystemErrorStream errorStream: ByteArrayOutputStream
    ) {
        testedHandler.handleLog(
            fakeLevel,
            fakeMessage,
            null,
            emptyMap(),
            emptySet()
        )

        val prefix = levels[fakeLevel]
        assertThat(outputStream.toString())
            .isEqualTo("$prefix/$fakeServiceName: $fakeMessage\n")
        assertThat(errorStream.toString())
            .isEmpty()
    }

    companion object {
        val levels = arrayOf("0", "1", "V", "D", "I", "W", "E", "A", "X")
    }
}
