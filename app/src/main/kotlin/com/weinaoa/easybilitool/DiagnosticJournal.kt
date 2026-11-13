/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import java.io.File

/** Two bounded UTF-8 files. Call from the diagnostic worker, never the draw/touch thread. */
internal class DiagnosticJournal(private val directory: File, private val limit: Int = 256 * 1024) {
    private val current = File(directory, "current.log")
    private val previous = File(directory, "previous.log")

    @Synchronized fun append(line: String) {
        check(directory.isDirectory || directory.mkdirs()) { "Diagnostic directory unavailable" }
        val bytes = (clean(line) + "\n").toByteArray(Charsets.UTF_8)
        require(bytes.size <= limit)
        if (current.length() + bytes.size > limit) {
            check(!previous.exists() || previous.delete())
            check(!current.exists() || current.renameTo(previous))
        }
        current.appendBytes(bytes)
    }

    @Synchronized fun snapshot(): String = buildString {
        for (file in listOf(previous, current)) {
            if (file.isFile) append(file.readText(Charsets.UTF_8))
        }
    }

    @Synchronized fun clear() {
        for (file in listOf(current, previous)) check(!file.exists() || file.delete())
    }

    companion object {
        fun clean(value: String): String = value.take(700).replace('\n', ' ').replace('\r', ' ')
            .replace(Regex("https?://\\S+"), "[url]")
            .replace(Regex("(?i)(cookie|authorization|access[_-]?token|sessdata)\\s*[:=]\\s*\\S+"), "$1=[redacted]")
    }
}
