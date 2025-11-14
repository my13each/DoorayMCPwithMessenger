import com.bifos.dooray.mcp.tools.*
import kotlinx.serialization.json.Json

fun main() {
    println("=== Tool 1: GetWikis ===")
    val tool1 = getWikisTool()
    println(Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), tool1.inputSchema.properties))

    println("\n=== Tool 17: SearchMembers ===")
    val tool17 = searchMembersTool()
    println(Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), tool17.inputSchema.properties))
}
