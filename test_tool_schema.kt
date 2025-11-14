import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun main() {
    // 현재 방식
    val tool = Tool(
        name = "test_tool",
        description = "테스트 도구",
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("param1") {
                        put("type", "string")
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("param1"))
                }
            }
        )
    )

    // Tool 객체의 inputSchema를 JSON으로 직렬화
    println("=== Tool inputSchema ===")
    println(Json.encodeToString(Tool.serializer(), tool))
}
