package net.spacetivity.ludo.field

import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.spacetivity.ludo.LudoGame
import net.spacetivity.ludo.utils.PathFace

class GameFieldPropertiesTypeAdapter : TypeAdapter<GameFieldProperties>() {

    override fun write(writer: JsonWriter, properties: GameFieldProperties) {
        writer.beginObject()

        writer.name("teamFieldIds")
        writer.value(LudoGame.GSON.toJson(properties.teamFieldIds))

        writer.name("teamEntrance")
        writer.value(if (properties.teamEntrance == null) "-" else properties.teamEntrance)

        writer.name("turnComponent")
        writer.value(if (properties.turnComponent == null) "-" else properties.turnComponent!!.name)

        writer.endObject()
    }

    override fun read(reader: JsonReader): GameFieldProperties {
        lateinit var teamFieldsIds: MutableMap<String, Int>
        var teamEntrance: String? = null
        var turnComponent: PathFace? = null

        reader.beginObject()

        var fieldName: String? = null

        while (reader.hasNext()) {
            val token: JsonToken = reader.peek()
            if (token == JsonToken.NAME) fieldName = reader.nextName()

            reader.peek()

            when (fieldName) {
                "teamFieldIds" -> teamFieldsIds = LudoGame.GSON.fromJson(reader.nextString(), object : TypeToken<MutableMap<String, Int>>() {}.type)
                "teamEntrance" -> teamEntrance = if (reader.nextString() == "-") null else reader.nextString()
                "turnComponent" -> turnComponent = if (reader.nextString() == "-") null else PathFace.valueOf(reader.nextString())
            }
        }

        reader.endObject()
        return GameFieldProperties(teamFieldsIds, teamEntrance, turnComponent)
    }

}