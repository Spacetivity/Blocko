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
    private val whitelistedBlockTypes = mapOf<Set<Material>, ScannerResult>(
        // garage fields
        Pair(setOf(Material.RED_CONCRETE, Material.GREEN_CONCRETE, Material.BLUE_CONCRETE, Material.YELLOW_CONCRETE), ScannerResult.GARAGE_FIELD),

        // game fields
        Pair(setOf(Material.RED_WOOL, Material.GREEN_WOOL, Material.BLUE_WOOL, Material.YELLOW_WOOL, Material.BONE_BLOCK), ScannerResult.GAME_FIELD),

        // team spawns
        Pair(setOf(Material.RED_GLAZED_TERRACOTTA, Material.GREEN_GLAZED_TERRACOTTA, Material.BLUE_GLAZED_TERRACOTTA, Material.YELLOW_GLAZED_TERRACOTTA), ScannerResult.TEAM_SPAWN)
    )

    /**
     * Scans the given rectangular region and detects whitelisted block types.
     *
     * @param arenaSetupData Data containing the team definitions for the arena setup.
     * @return A mutable map where each entry maps a [Location] to a [ScannerResult] and the associated team name.
     */
    fun scanRegion(arenaSetupData: GameArenaSetupData): MutableMap<Location, Pair<ScannerResult, String>> {
        val corner1 = arenaSetupData.corner1 ?: return mutableMapOf()
        val corner2 = arenaSetupData.corner2 ?: return mutableMapOf()

        val minX = corner1.blockX.coerceAtMost(corner2.blockX)
        val maxX = corner1.blockX.coerceAtLeast(corner2.blockX)

        val minZ = corner1.blockZ.coerceAtMost(corner2.blockZ)
        val maxZ = corner1.blockZ.coerceAtLeast(corner2.blockZ)

        val resultsInRegion = mutableMapOf<Location, Pair<ScannerResult, String>>()

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                val location = Location(corner1.world, x.toDouble(), corner1.blockY.toDouble(), z.toDouble())

                val blockType = location.block.type

                val materials: Set<Material> = this.whitelistedBlockTypes.keys.first { it.contains(blockType) }
                if (materials.isEmpty()) continue

                val teamName = blockType.name.split("_")[0]
                if (arenaSetupData.gameTeams.none { team -> team.name == teamName }) continue

                val scannerResult: ScannerResult = this.whitelistedBlockTypes[materials] ?: continue

                resultsInRegion[location] = Pair(scannerResult, teamName)
            }
        }

        return resultsInRegion
    }

}

enum class ScannerResult {
    GARAGE_FIELD,
    GAME_FIELD,
    TEAM_SPAWN
}