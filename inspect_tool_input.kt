import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

fun main() {
    // Tool.Input의 구조를 확인
    val testSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("testParam") {
                put("type", "string")
            }
        }
    }

    val toolInput = Tool.Input(properties = testSchema)

    println("Tool.Input fields:")
    println("  properties = ${ toolInput.properties}")

    // 직렬화해서 실제 JSON 구조 확인
    println("\nSerialized:")
    println(Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), toolInput.properties))
}
