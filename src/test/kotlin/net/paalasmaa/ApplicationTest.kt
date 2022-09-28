package net.paalasmaa

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.locations.*
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.databind.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import net.paalasmaa.plugins.*


fun myTestApplication(block: suspend ApplicationTestBuilder.() -> Unit) {
    testApplication {
        application {
            configureRouting()
        }

        block()
    }
}

class ApplicationTest {
    @Test
    fun testInvalidType() = myTestApplication {
        client.post("/openinghours") {
            setBody("""'""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("Malformed JSON", bodyAsText())
        }
    }

    @Test
    fun testInvalidValueTooSmall() = myTestApplication {
        client.post("/openinghours") {
            setBody("""{
                "monday": [],
                "tuesday": [ 
                    {
                        "type": "open",
                        "value": -1
                    }
                ],
                "wednesday": [], "thursday": [], "friday": [], "saturday": [], "sunday": []}""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertEquals("The time values must be in range 0...86400", bodyAsText())
        }
    }

    @Test
    fun testRoot2() = myTestApplication {
        client.post("/openinghours") {
            setBody("""
{
    "monday" : [],
    "tuesday" : [ 
        {
            "type" : "open",
            "value" : 36000
        }, 
        {
            "type" : "close",
            "value" : 64800
        }
    ],
    "wednesday" : [],
    "thursday" : [ 
        {
            "type" : "open",
            "value" : 37800
        }, 
        {
            "type" : "close",
            "value" : 64800
        }
    ],
    "friday" : [ 
        {
            "type" : "open",
            "value" : 36000
        } 
    ],
    "saturday" : [ 
        {
            "type" : "close",
            "value" : 3600
        },
        {
            "type" : "open",
            "value" : 36000
        }
    ],
    "sunday" : [
         {
            "type" : "close",
            "value" : 3600
        },
        {
            "type" : "open",
            "value" : 43200
        }, 
        {
            "type" : "close",
            "value" : 75600
        }
    ]
}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""Monday: Closed
Tuesday: 10:0 AM - 6:0 PM
Wednesday: Closed
Thursday: 10:30 AM - 6:0 PM
Friday: 10:0 AM - 1:0 AM
Saturday: 10:0 AM - 1:0 AM
Sunday: 12:0 PM - 9:0 PM""", bodyAsText())
        }
    }
}