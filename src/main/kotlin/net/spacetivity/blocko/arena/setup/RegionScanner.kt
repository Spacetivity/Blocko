package net.spacetivity.blocko.arena.setup

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.bukkit.Location
import org.bukkit.Material

/**
 * Scans a rectangular region in the world and identifies special blocks based on predefined types.
 * These are used to determine garage fields, team start fields, and team spawns.
 */
object RegionScanner {

    /**
     * Maps sets of materials to their corresponding [ScannerResult] type.
     * Each set represents a category (e.g., garage fields, team start fields) with team colors.
     */
    private val blocksOfScannableType = mapOf(
        // team spawns
        Pair(setOf(
            Material.RED_GLAZED_TERRACOTTA,
            Material.GREEN_GLAZED_TERRACOTTA,
            Material.BLUE_GLAZED_TERRACOTTA,
            Material.YELLOW_GLAZED_TERRACOTTA),
            ScannerResult.TEAM_SPAWN
        ),

        // game fields
        Pair(setOf(
            Material.RED_WOOL,
            Material.GREEN_WOOL,
            Material.BLUE_WOOL,
            Material.YELLOW_WOOL,
            Material.BONE_BLOCK,
            Material.RED_CONCRETE,
            Material.GREEN_CONCRETE,
            Material.BLUE_CONCRETE,
            Material.YELLOW_CONCRETE
        ), ScannerResult.GAME_FIELD),

        // garage fields
        Pair(setOf(
            Material.RED_CONCRETE,
            Material.GREEN_CONCRETE,
            Material.BLUE_CONCRETE,
            Material.YELLOW_CONCRETE
        ), ScannerResult.GARAGE_FIELD)
    )

    fun scanRegion(arenaSetupData: GameArenaSetupData): Multimap<Location, Pair<ScannerResult, String?>> {
        val resultsInRegion: Multimap<Location, Pair<ScannerResult, String?>> = ArrayListMultimap.create()

        val corner1 = arenaSetupData.corner1 ?: return resultsInRegion
        val corner2 = arenaSetupData.corner2 ?: return resultsInRegion

        val minX = corner1.blockX.coerceAtMost(corner2.blockX)
        val maxX = corner1.blockX.coerceAtLeast(corner2.blockX)

        val minZ = corner1.blockZ.coerceAtMost(corner2.blockZ)
        val maxZ = corner1.blockZ.coerceAtLeast(corner2.blockZ)

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val location = Location(corner1.world, x.toDouble(), corner1.blockY.toDouble(), z.toDouble())
                val blockType: Material = location.block.type

                val categoriesCurrentBlockTypeIsRegisteredIn: List<Map.Entry<Set<Material>, ScannerResult>> = this.blocksOfScannableType.entries.filter { it.key.contains(blockType) }
                if (categoriesCurrentBlockTypeIsRegisteredIn.isEmpty()) continue

                for (categoryCurrentBlockTypeIsRegisteredIn: Map.Entry<Set<Material>, ScannerResult> in categoriesCurrentBlockTypeIsRegisteredIn) {
                    val scannerResult = categoryCurrentBlockTypeIsRegisteredIn.value
                    resultsInRegion.put(location, Pair(scannerResult, getPossibleTeamName(scannerResult, blockType, arenaSetupData)))
                }
            }
        }

        return resultsInRegion
    }

    private fun getPossibleTeamName(scannerResult: ScannerResult, currentBlockType: Material, arenaSetupData: GameArenaSetupData): String? {
        val isTeamResult = scannerResult == ScannerResult.TEAM_SPAWN || scannerResult == ScannerResult.GARAGE_FIELD
        val teamName = if (isTeamResult) currentBlockType.name.split("_")[0].lowercase() else null
        return if (teamName != null && arenaSetupData.gameTeams.none { team -> team.name.equals(teamName, true) }) null else teamName
    }
}

enum class ScannerResult(val priority: Int) {
    GARAGE_FIELD(2),
    GAME_FIELD(1),
    TEAM_SPAWN(0),
    UNKNOWN(-1);

    companion object {
        fun getMissingResults(results: Collection<ScannerResult>): Map<ScannerResult, Int> {
            val missingResults: MutableMap<ScannerResult, Int> = mutableMapOf()

            val neededGarageFieldAmount = 16
            val neededGameFieldAmount = 40
            val neededTeamSpawnAmount = 16

            val missingGarageFieldAmount = neededGarageFieldAmount - results.count { it == GARAGE_FIELD }
            val missingGameFieldAmount = neededGameFieldAmount - results.count { it == GAME_FIELD }
            val missingTeamSpawnFieldAmount = neededTeamSpawnAmount - results.count { it == TEAM_SPAWN }

            if (missingGarageFieldAmount > 0) missingResults[GARAGE_FIELD] = missingGarageFieldAmount
            if (missingGameFieldAmount > 0) missingResults[GAME_FIELD] = missingGameFieldAmount
            if (missingTeamSpawnFieldAmount > 0) missingResults[TEAM_SPAWN] = missingTeamSpawnFieldAmount

            return missingResults
        }

        fun containsAllValidResults(results: Collection<ScannerResult>): Boolean {
            return results.filter { it != UNKNOWN }.containsAll(entries.filter { it != UNKNOWN })
        }
    }
}