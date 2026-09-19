package com.tungsten.hfclcore.mod.modinfo

import com.google.gson.JsonParseException
import com.tungsten.hfclcore.util.gson.Validation

data class JarInJarMetadata(var jars: MutableList<EmbeddedJarMetadata>? = null) : Validation {
    @Throws(JsonParseException::class)
    override fun validate() {
        Validation.requireNonNull(jars, "jars")
        for (jar in jars!!) {
            jar.validate()
        }
    }
}
