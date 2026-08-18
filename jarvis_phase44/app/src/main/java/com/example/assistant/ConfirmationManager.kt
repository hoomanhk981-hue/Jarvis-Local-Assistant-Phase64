package com.example.assistant

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Central approval gate for actions that can change device state, communicate externally,
 * expose sensitive data, or move money. Requests are single-use and expire quickly.
 */
class ConfirmationManager(
    private val ttlMillis: Long = 2 * 60 * 1000L
) {
    enum class Risk { LOW, MEDIUM, HIGH, CRITICAL }

    data class Request(
        val id: String,
        val toolName: String,
        val arguments: Map<String, String>,
        val summary: String,
        val risk: Risk,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = createdAt + ttlMillis
    )

    private val pending = ConcurrentHashMap<String, Request>()

    fun create(toolName: String, arguments: Map<String, String>, summary: String, risk: Risk): Request {
        val request = Request(UUID.randomUUID().toString(), toolName, arguments.toMap(), summary, risk)
        pending[request.id] = request
        return request
    }

    fun get(id: String): Request? {
        val request = pending[id] ?: return null
        if (request.expiresAt < System.currentTimeMillis()) {
            pending.remove(id)
            return null
        }
        return request
    }

    fun approve(id: String): Request? = get(id)?.also { pending.remove(id) }

    fun reject(id: String): Request? = pending.remove(id)

    fun clear() = pending.clear()

    companion object {
        fun riskForTool(toolName: String, arguments: Map<String, String>): Risk = when (toolName) {
            "transfer_money", "submit_payment", "bank_transfer" -> Risk.CRITICAL
            "send_sms", "make_call", "delete_file", "run_termux" ->
                if (toolName == "run_termux" && isDangerousCommand(arguments["command"].orEmpty())) Risk.HIGH else Risk.MEDIUM
            "remember", "write_file", "browser_type", "browser_click", "accessibility_action" -> Risk.MEDIUM
            else -> Risk.LOW
        }

        private fun isDangerousCommand(command: String): Boolean {
            val normalized = command.lowercase()
            val patterns = listOf(
                "rm -rf", "rm -r /", "mkfs", "dd if=", "shutdown", "reboot",
                "factory-reset", "wipe", "chmod 777", "chown -r", "> /dev/"
            )
            return patterns.any(normalized::contains)
        }
    }
}
