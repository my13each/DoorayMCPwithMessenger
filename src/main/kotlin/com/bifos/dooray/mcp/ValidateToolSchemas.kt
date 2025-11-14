package com.bifos.dooray.mcp

import com.bifos.dooray.mcp.tools.*
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun main() {
    val tools = getAllTools()
    
    println("\n=== Validating ${tools.size} tools ===\n")
    
    var issueCount = 0
    
    tools.forEachIndexed { index, tool ->
        println("[$index] ${tool.name}")
        val hasIssue = validateToolSchema(tool)
        if (hasIssue) issueCount++
        println()
    }
    
    println("\n=== Summary ===")
    println("Total tools: ${tools.size}")
    println("Tools with issues: $issueCount")
}

fun validateToolSchema(tool: Tool): Boolean {
    val schema = tool.inputSchema?.properties
    
    if (schema == null) {
        println("  ⚠️  No input schema")
        return true
    }
    
    var hasIssue = false
    
    // Check for required fields at root level
    val hasTypeAtRoot = schema.containsKey("type")
    val hasPropertiesAtRoot = schema.containsKey("properties")
    
    println("  - Has 'type' at root: $hasTypeAtRoot")
    println("  - Has 'properties' at root: $hasPropertiesAtRoot")
    
    if (hasTypeAtRoot && hasPropertiesAtRoot) {
        // Validate type value
        val typeValue = schema["type"]?.jsonPrimitive?.content
        if (typeValue == "object") {
            println("  ✅ type = 'object'")
        } else {
            println("  ❌ type should be 'object', but is: $typeValue")
            hasIssue = true
        }
        
        // Check properties structure
        val properties = schema["properties"]
        if (properties is JsonObject) {
            println("  ✅ properties is a JsonObject with ${properties.size} fields")
        } else {
            println("  ❌ properties should be a JsonObject")
            hasIssue = true
        }
        
        // Check required array if present
        val required = schema["required"]
        if (required is JsonArray) {
            println("  ✅ required: ${required.map { it.jsonPrimitive.content }}")
        } else if (required != null) {
            println("  ❌ required should be a JsonArray")
            hasIssue = true
        }
    } else {
        println("  ❌ Schema is missing 'type' and/or 'properties' at root")
        println("  Schema keys: ${schema.keys}")
        hasIssue = true
    }
    
    return hasIssue
}

fun getAllTools(): List<Tool> {
    return listOf(
        getWikisTool(),
        getWikiPagesTool(),
        getWikiPageTool(),
        createWikiPageTool(),
        updateWikiPageTool(),
        getProjectPostsTool(),
        getProjectPostTool(),
        createProjectPostTool(),
        setProjectPostWorkflowTool(),
        setProjectPostDoneTool(),
        getProjectsTool(),
        updateProjectPostTool(),
        createPostCommentTool(),
        getPostCommentsTool(),
        updatePostCommentTool(),
        deletePostCommentTool(),
        searchMembersTool(),
        sendDirectMessageTool(),
        getChannelsTool(),
        getSimpleChannelsTool(),
        getChannelTool(),
        sendChannelMessageTool(),
        createChannelTool(),
        getCalendarsTool(),
        getCalendarDetailTool(),
        getCalendarEventsTool(),
        getCalendarEventDetailTool(),
        createCalendarEventTool(),
        getDrivesTool(),
        getDriveFilesTool(),
        uploadFileFromPathTool(),
        uploadFileTool(),
        downloadFileTool(),
        getFileMetadataTool(),
        updateFileTool(),
        moveFileToTrashTool(),
        deleteFileTool(),
        createFolderTool(),
        copyFileTool(),
        moveFileTool(),
        createSharedLinkTool(),
        getSharedLinksTool(),
        getSharedLinkDetailTool(),
        updateSharedLinkTool(),
        deleteSharedLinkTool()
    )
}
