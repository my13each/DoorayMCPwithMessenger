import kotlinx.serialization.json.*

fun main() {
    val schema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("organization_member_id") {
                put("type", "string")
                put("description", "메시지를 받을 멤버의 조직 멤버 ID")
            }
            putJsonObject("text") {
                put("type", "string")
                put("description", "전송할 메시지 내용")
            }
        }
        putJsonArray("required") {
            add("organization_member_id")
            add("text")
        }
    }

    println(Json.encodeToString(JsonObject.serializer(), schema))
}
