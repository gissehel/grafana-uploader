package io.github.gissehel.grafana.grafanauploader.tools

import kotlinx.serialization.json.*

fun JsonElement.asArray(): List<JsonElement> {
    try {
        return this.jsonArray.map { it }
    } catch (e: IllegalArgumentException) {
        assert(false) {
            "json element ${this.toString()} should be an array"
        }
    }
    return emptyList()
}

fun JsonElement.asObject(): JsonObject {
    try {
        return this.jsonObject
    } catch (e: IllegalArgumentException) {
        assert(false) {
            "json element ${this.toString()} should be an object"
        }
    }
    return this.jsonObject
}

fun JsonElement.asString(): String {
    try {
        assert(this.jsonPrimitive.isString) {
            "json element ${this.toString()} should be a primitive String"
        }
    } catch (e: IllegalArgumentException) {
        assert(false) {
            "json element ${this.toString()} should be a primitive"
        }
    }

    return this.jsonPrimitive.content
}

