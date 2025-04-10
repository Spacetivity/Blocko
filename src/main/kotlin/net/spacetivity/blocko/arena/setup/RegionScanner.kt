package net.spacetivity.blocko.arena.setup

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
    private val fieldTypeByTypes = mapOf<Set<Material>, ScannerResult>(
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

    /**
     * Scans the given rectangular region and detects whitelisted block types.
     *
     * @param arenaSetupData Data containing the team definitions for the arena setup.
     * @return A mutable map where each entry maps a [Location] to a [ScannerResult] and the associated team name.
     */


    // NOTE: the results need to be in order
    // 1. GAME_FIELD then 2. GARAGE_FIELD
    fun scanRegion(arenaSetupData: GameArenaSetupData): MutableMap<Location, Pair<ScannerResult, String?>> {
        val corner1 = arenaSetupData.corner1 ?: return mutableMapOf()
        val corner2 = arenaSetupData.corner2 ?: return mutableMapOf()

        val minX = corner1.blockX.coerceAtMost(corner2.blockX)
        val maxX = corner1.blockX.coerceAtLeast(corner2.blockX)

        val minZ = corner1.blockZ.coerceAtMost(corner2.blockZ)
        val maxZ = corner1.blockZ.coerceAtLeast(corner2.blockZ)

        // this currently stays empty
        val resultsInRegion = mutableMapOf<Location, Pair<ScannerResult, String?>>()

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val location = Location(corner1.world, x.toDouble(), corner1.blockY.toDouble(), z.toDouble())
                val blockType: Material = location.block.type

                val validBlockTypes: Set<Material> = this.fieldTypeByTypes.keys.firstOrNull { it.contains(blockType) } ?: emptySet()
                if (validBlockTypes.isEmpty()) continue

                // there can be more than one scanner result for a block (e.g. GAME_FIELD & GARAGE_FIELD)
                var scannerResult: ScannerResult = ScannerResult.UNKNOWN

                for (fieldTypeByType: Map.Entry<Set<Material>, ScannerResult> in this.fieldTypeByTypes) {
                    for (blockType in validBlockTypes) {
                        val whitelistedBlockTypes: Set<Material> = fieldTypeByType.key
                        if (!whitelistedBlockTypes.contains(blockType)) continue
                        scannerResult = fieldTypeByType.value
                    }
                }

                val isTeamResult = scannerResult == ScannerResult.TEAM_SPAWN || scannerResult == ScannerResult.GARAGE_FIELD
                val teamName = if (isTeamResult) blockType.name.split("_")[0] else null
                if (teamName != null && arenaSetupData.gameTeams.none { team -> team.name.equals(teamName, true) }) continue

                resultsInRegion[location] = Pair(scannerResult, teamName)
            }
        }

        return resultsInRegion
    }

}

enum class ScannerResult(val priority: Int) {
    GARAGE_FIELD(2),
    GAME_FIELD(1),
    TEAM_SPAWN(0),
    UNKNOWN(-1)
}