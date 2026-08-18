package com.example.assistant

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.models.AccessibilitySnapshot

/**
 * User-enabled UI automation bridge.
 *
 * This service never runs autonomously from accessibility events. The Agent must
 * explicitly request an action through ToolRegistry, and mutating actions are
 * gated by the central ConfirmationManager.
 */
class JarvisAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() = Unit
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

    fun snapshot(): AccessibilitySnapshot {
        val root = rootInActiveWindow ?: return AccessibilitySnapshot(null, emptyList())
        val nodes = mutableListOf<String>()
        collect(root, nodes, 0)
        return AccessibilitySnapshot(root.packageName?.toString(), nodes.take(400))
    }

    private fun collect(node: AccessibilityNodeInfo, out: MutableList<String>, depth: Int) {
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        val id = node.viewIdResourceName.orEmpty()
        if (text.isNotBlank() || desc.isNotBlank() || id.isNotBlank()) {
            out += "depth=$depth text=${text.take(160)} desc=${desc.take(160)} id=$id " +
                "clickable=${node.isClickable} editable=${node.isEditable} enabled=${node.isEnabled}"
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collect(child, out, depth + 1)
                child.recycle()
            }
        }
    }

    fun performToolAction(action: String, args: Map<String, String>): ToolResult {
        val root = rootInActiveWindow ?: return ToolResult.Failure("No active window")
        return when (action) {
            "inspect" -> ToolResult.Success(
                "Active UI inspected",
                mapOf(
                    "package" to (root.packageName?.toString() ?: ""),
                    "snapshot" to snapshot().nodes.joinToString("\n")
                )
            )
            "click" -> {
                val node = findNode(root, args)
                    ?: return ToolResult.Failure("UI element not found")
                if (!node.isEnabled) return ToolResult.Failure("UI element is disabled")
                val ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                node.recycle()
                if (ok) ToolResult.Success("UI element clicked")
                else ToolResult.Failure("Click failed")
            }
            "set_text" -> {
                val value = args["value"] ?: return ToolResult.Failure("value is required")
                val node = findNode(root, args) ?: findFirstEditable(root)
                    ?: return ToolResult.Failure("Editable field not found")
                if (!node.isEditable) {
                    node.recycle()
                    return ToolResult.Failure("Target is not editable")
                }
                val b = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        value
                    )
                }
                val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
                node.recycle()
                if (ok) ToolResult.Success("Text entered")
                else ToolResult.Failure("Could not set text")
            }
            "back" -> if (performGlobalAction(GLOBAL_ACTION_BACK))
                ToolResult.Success("Back pressed")
            else ToolResult.Failure("Back failed")
            else -> ToolResult.Failure("Unknown accessibility action: $action")
        }
    }

    private fun findNode(root: AccessibilityNodeInfo, args: Map<String, String>): AccessibilityNodeInfo? {
        val text = args["text"]?.trim().orEmpty()
        val content = args["content_description"]?.trim().orEmpty()
        val viewId = args["view_id"]?.trim().orEmpty()

        if (viewId.isNotBlank() && root.viewIdResourceName == viewId) return root
        if (content.isNotBlank() && root.contentDescription?.toString()?.trim() == content) return root
        if (text.isNotBlank() && root.text?.toString()?.trim() == text) return root

        for (i in 0 until root.childCount) {
            root.getChild(i)?.let { child ->
                val found = findNode(child, args)
                if (found != null) return found
                child.recycle()
            }
        }
        return null
    }

    private fun findFirstEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable && root.isEnabled) return root
        for (i in 0 until root.childCount) {
            root.getChild(i)?.let { child ->
                val found = findFirstEditable(child)
                if (found != null) return found
                child.recycle()
            }
        }
        return null
    }
}
