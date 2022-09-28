package net.paalasmaa.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import com.fasterxml.jackson.module.kotlin.*
import net.paalasmaa.*


fun handleRequest(requestBody: String): Result<String> {
    var deserialized: InputType = mapOf()
    try {
        val mapper = jacksonObjectMapper()
        deserialized = mapper.readValue(requestBody)
    } catch (error: com.fasterxml.jackson.databind.exc.InvalidFormatException) {
        return Result.failure(Exception("JSON does not conform to the schema"))
    } catch (error: com.fasterxml.jackson.core.JsonParseException) {
        return Result.failure(Exception("Malformed JSON"))
    }

    transformToOpenPeriods(deserialized).fold(
        onSuccess = {
            val formattedDays = formatOpeningHours(it)
            return Result.success(formattedDays.joinToString("\n"))
        },
        onFailure = {
            return Result.failure(it)
        }
    )
}


fun Application.configureRouting() {
    routing {
        post("/openinghours") {
            handleRequest(call.receiveText()).fold(
                onSuccess = {
                    call.respondText(it)
                },
                onFailure = {
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respondText(it.message ?: "")
                }
            )
        }
    }
}
