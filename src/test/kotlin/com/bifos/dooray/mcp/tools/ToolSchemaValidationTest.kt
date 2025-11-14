package com.bifos.dooray.mcp.tools

import com.bifos.dooray.mcp.client.DoorayHttpClient
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * 각 도구의 JSON Schema가 올바른 구조인지 검증하는 테스트
 */
class ToolSchemaValidationTest {

    @Test
    fun `validate all tool schemas`() {
        val tools = getAllTools()
        
        println("\n=== Validating ${tools.size} tools ===\n")
        
        tools.forEachIndexed { index, tool ->
            println("[$index] ${tool.name}")
            validateToolSchema(tool)
            println()
        }
    }
    
    @Test
    fun `print all tool schemas`() {
        val tools = getAllTools()
        
        tools.forEachIndexed { index, tool ->
            println("\n=== Tool #$index: ${tool.name} ===")
            println("Description: ${tool.description}")
            
            val schema = tool.inputSchema?.properties
            if (schema != null) {
                println("Input Schema:")
                println(Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), schema))
            } else {
                println("No input schema")
            }
        }
    }
    
    private fun validateToolSchema(tool: Tool) {
        val schema = tool.inputSchema?.properties
        
        if (schema == null) {
            println("  ⚠️  No input schema")
            return
        }
        
        // Check if schema is a valid JSON object
        assertTrue(schema is JsonObject, "Schema must be a JsonObject")
        
        // Check for required fields at root level
        val hasTypeAtRoot = schema.containsKey("type")
        val hasPropertiesAtRoot = schema.containsKey("properties")
        
        println("  - Has 'type' at root: $hasTypeAtRoot")
        println("  - Has 'properties' at root: $hasPropertiesAtRoot")
        
        if (hasTypeAtRoot && hasPropertiesAtRoot) {
            println("  ✅ Schema structure looks correct (type + properties at root)")
            
            // Validate type value
            val typeValue = schema["type"]?.jsonPrimitive?.content
            if (typeValue == "object") {
                println("  ✅ type = 'object' is correct")
            } else {
                println("  ❌ type should be 'object', but is: $typeValue")
            }
            
            // Check properties structure
            val properties = schema["properties"]
            if (properties is JsonObject) {
                println("  ✅ properties is a JsonObject with ${properties.size} fields")
                properties.keys.forEach { key ->
                    println("      - $key")
                }
            } else {
                println("  ❌ properties should be a JsonObject")
            }
            
            // Check required array if present
            val required = schema["required"]
            if (required is JsonArray) {
                println("  ✅ required is a JsonArray with ${required.size} items: ${required.map { it.jsonPrimitive.content }}")
            } else if (required != null) {
                println("  ❌ required should be a JsonArray")
            }
        } else {
            println("  ⚠️  Unexpected schema structure - might be nested incorrectly")
            println("  Schema keys: ${schema.keys}")
        }
    }
    
    private fun getAllTools(): List<Tool> {
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
}
