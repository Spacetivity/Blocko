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

        writer.name("garageForTeam")
        writer.value(if (properties.garageForTeam == null) "-" else properties.garageForTeam)

        writer.name("turnComponent")
        writer.value(if (properties.turnComponent == null) "-" else properties.turnComponent!!.name)

        writer.endObject()
    }

    override fun read(reader: JsonReader): GameFieldProperties {
        lateinit var teamFieldsIds: MutableMap<String, Int>
        var garageForTeam: String? = null
        var turnComponent: PathFace? = null

        reader.beginObject()

        var fieldName: String? = null

        while (reader.hasNext()) {
            val token: JsonToken = reader.peek()
            if (token == JsonToken.NAME) fieldName = reader.nextName()

            when (fieldName) {
                "teamFieldIds" -> {
                    reader.peek()
                    teamFieldsIds = LudoGame.GSON.fromJson(reader.nextString(), object : TypeToken<MutableMap<String, Int>>() {}.type)
                }
                "garageForTeam" -> {
                    reader.peek()
                    val garageValue: String = reader.nextString()
                    garageForTeam = if (garageValue == "-") null else garageValue
                }
                "turnComponent" -> {
                    reader.peek()
                    val turnComponentValue: String = reader.nextString()
                    turnComponent = if (turnComponentValue == "-") null else PathFace.valueOf(turnComponentValue)
                }
            }
        }

        reader.endObject()
        return GameFieldProperties(teamFieldsIds, garageForTeam, turnComponent)
    }

}