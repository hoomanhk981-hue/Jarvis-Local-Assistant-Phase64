package com.example.termux

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

object TermuxResultBroker {
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<TermuxCommandResult>>()

    fun register(executionId: Int): CompletableDeferred<TermuxCommandResult> {
        val deferred = CompletableDeferred<TermuxCommandResult>()
        pending[executionId] = deferred
        return deferred
    }

    fun complete(result: TermuxCommandResult) {
        pending.remove(result.executionId)?.complete(result)
    }

    fun cancel(executionId: Int) {
        pending.remove(executionId)?.cancel()
    }
}
