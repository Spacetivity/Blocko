package net.spacetivity.blocko.field

import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.spacetivity.blocko.BlockoGame

class GameFieldPropertiesTypeAdapter : TypeAdapter<GameFieldProperties>() {

    override fun write(writer: JsonWriter, properties: GameFieldProperties) {
        writer.beginObject()

        writer.name("teamFieldIds")
        writer.value(BlockoGame.GSON.toJson(properties.teamFieldIds))

        writer.name("garageForTeam")
        writer.value(if (properties.garageForTeam == null) "-" else properties.garageForTeam)

        writer.name("teamEntrance")
        writer.value(if (properties.teamEntrance == null) "-" else properties.teamEntrance)

        writer.name("turnComponent")
        writer.value(if (properties.rotation == null) "-" else properties.rotation!!.name)

        writer.endObject()
    }

    override fun read(reader: JsonReader): GameFieldProperties {
        lateinit var teamFieldsIds: MutableMap<String, Int>
        var garageForTeam: String? = null
        var teamEntrance: String? = null
        var turnComponent: PathFace? = null

        reader.beginObject()

        var fieldName: String? = null

        while (reader.hasNext()) {
            val token: JsonToken = reader.peek()
            if (token == JsonToken.NAME) fieldName = reader.nextName()

            when (fieldName) {
                "teamFieldIds" -> {
                    reader.peek()
                    teamFieldsIds = BlockoGame.GSON.fromJson(reader.nextString(), object : TypeToken<MutableMap<String, Int>>() {}.type)
                }
                "garageForTeam" -> {
                    reader.peek()
                    val garageValue: String = reader.nextString()
                    garageForTeam = if (garageValue == "-") null else garageValue
                }
                "teamEntrance" -> {
                    reader.peek()
                    val teamEntranceValue: String = reader.nextString()
                    teamEntrance = if (teamEntranceValue == "-") null else teamEntranceValue
                }
                "turnComponent" -> {
                    reader.peek()
                    val turnComponentValue: String = reader.nextString()
                    turnComponent = if (turnComponentValue == "-") null else PathFace.valueOf(turnComponentValue)
                }
            }
        }

        reader.endObject()
        return GameFieldProperties(teamFieldsIds, garageForTeam, teamEntrance, turnComponent)
    }

}