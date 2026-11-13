/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import java.nio.file.Files

object DiagnosticJournalTest {
    @JvmStatic fun main(args: Array<String>) {
        val directory = Files.createTempDirectory("module-journal-test").toFile()
        try {
            val journal = DiagnosticJournal(directory, 2048)
            journal.append("old session")
            check(DiagnosticJournal(directory, 2048).snapshot().contains("old session"))
            repeat(100) { journal.append("event=$it " + "界".repeat(100)) }
            check(directory.listFiles()!!.sumOf { it.length() } <= 4096)
            val snapshot = journal.snapshot()
            check("event=99 " in snapshot && "event=0 " !in snapshot && '\uFFFD' !in snapshot)
            val clean = DiagnosticJournal.clean("auth https://example.org/a?key=private\nCookie=secret SESSDATA=private")
            check("secret" !in clean && "example.org" !in clean && "SESSDATA=private" !in clean && '\n' !in clean)
            val threads = (1..4).map { id -> Thread { repeat(30) { journal.append("thread=$id line=$it") } }.apply { start() } }
            threads.forEach { it.join() }
            check(journal.snapshot().lineSequence().filter { it.isNotEmpty() }.all { it.startsWith("thread=") || it.startsWith("event=") })
            journal.clear(); check(journal.snapshot().isEmpty())
            println("DiagnosticJournal: UTF-8 rotation, restart retention, sanitizing, bounded storage, concurrent append and clear passed")
        } finally { directory.listFiles()?.forEach { it.delete() }; directory.delete() }
    }
}
