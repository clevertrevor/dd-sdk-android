/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.android.core.internal.data.file

import com.datadog.android.core.internal.data.Orchestrator
import com.datadog.android.core.internal.data.Reader
import com.datadog.android.core.internal.domain.Batch
import com.datadog.android.log.internal.utils.sdkLogger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal class FileReader(
    private val fileOrchestrator: Orchestrator,
    private val dataDirectory: File
) : Reader {

    private val sentBatches: MutableSet<String> = mutableSetOf()

    // region LogReader

    override fun readNextBatch(): Batch? {
        var file: File? = null
        val data = try {
            file = fileOrchestrator.getReadableFile(sentBatches) ?: return null
            file.readBytes(withPrefix = '[', withSuffix = ']')
        } catch (e: FileNotFoundException) {
            sdkLogger.e("$TAG: Couldn't create an input stream from file ${file?.path}", e)
            ByteArray(0)
        } catch (e: IOException) {
            sdkLogger.e("$TAG: Couldn't read logs from file ${file?.path}", e)
            ByteArray(0)
        } catch (e: SecurityException) {
            sdkLogger.e("$TAG: Couldn't access file ${file?.path}", e)
            ByteArray(0)
        }

        return if (file == null) {
            null
        } else {
            Batch(
                file.name,
                data)
        }
    }

    override fun dropBatch(batchId: String) {
        sdkLogger.i("$TAG: dropBatch $batchId")
        sentBatches.add(batchId)
        val fileToDelete = File(dataDirectory, batchId)

        deleteFile(fileToDelete)
    }

    override fun dropAllBatches() {
        sdkLogger.i("$TAG: dropAllBatches")
        fileOrchestrator.getAllFiles().forEach { deleteFile(it) }
    }

    // endregion

    // region Internal
    private fun deleteFile(fileToDelete: File) {
        if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
                sdkLogger.d("$TAG: File ${fileToDelete.path} deleted")
            } else {
                sdkLogger.e("$TAG: Error deleting file ${fileToDelete.path}")
            }
        } else {
            sdkLogger.w("$TAG: file ${fileToDelete.path} does not exist")
        }
    }

    // endregion

    companion object {
        private const val TAG = "FileReader"
    }
}
