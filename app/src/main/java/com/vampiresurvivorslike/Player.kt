package com.vampiresurvivorslike



import kotlin.random.Random

/**
 * Represents the different types of weapons available in the game.
 */
enum class WeaponType {
    SWORD,    // 검
    AXE,      // 도끼
    BOW,      // 활
    TALISMAN  // 부적
}

/**
 * Data class describing an upgrade option presented to the player when leveling up.
 *
 * @property weapon the weapon type this upgrade applies to
 * @property path   which upgrade path (1 or 2) is being offered
 * @property description a human‑readable description of what this upgrade does
 */
data class UpgradeOption(val weapon: WeaponType, val path: Int, val description: String)

/**
 * Base class for all weapons. Each weapon keeps track of two independent upgrade
 * paths (path1 and path2) which can each be leveled up to a maximum of three.
 * Concrete subclasses should override behaviour specific to that weapon type.
 */
abstract class Weapon {
    /** Current level of the first upgrade path for this weapon (0–3). */
    var path1Level: Int = 0
        private set

    /** Current level of the second upgrade path for this weapon (0–3). */
    var path2Level: Int = 0
        private set

    /** The unique type identifier for this weapon. */
    abstract val type: WeaponType

    /**
     * Base damage inflicted by this weapon before any upgrades. Concrete
     * implementations should return the starting damage defined by the game design.
     */
    abstract fun baseDamage(): Int

    /**
     * Increase the first upgrade path by one level, up to a maximum of three.
     * Each subclass may override this method to apply additional side effects
     * when an upgrade occurs (such as adjusting timers or damage multipliers).
     */
    open fun upgradePath1() {
        if (path1Level < 3) {
            path1Level++
        }
    }

    /**
     * Increase the second upgrade path by one level, up to a maximum of three.
     * Each subclass may override this method to apply additional side effects
     * when an upgrade occurs.
     */
    open fun upgradePath2() {
        if (path2Level < 3) {
            path2Level++
        }
    }

    /**
     * Apply an upgrade option to this weapon. Delegates to the appropriate
     * upgrade path based on the `path` value of the option.
     *
     * @param option the upgrade that has been chosen by the player
     */
    fun applyUpgrade(option: UpgradeOption) {
        when (option.path) {
            1 -> upgradePath1()
            2 -> upgradePath2()
        }
    }
}

/**
 * Implementation of the Sword weapon. In the design this weapon spins around
 * the player and has upgrades that increase the number of swords and the size
 * and damage of each sword.
 */
class Sword : Weapon() {
    override val type: WeaponType = WeaponType.SWORD

    /**
     * The base damage for the Sword before any upgrades (25 as per design).
     */
    override fun baseDamage(): Int = 25

    /**
     * Override path1 upgrade to increase the number of swords. Additional logic
     * such as adjusting spin speed would be implemented in the game loop.
     */
    override fun upgradePath1() {
        super.upgradePath1()
        // Additional sword‑specific effects could be placed here.
    }

    /**
     * Override path2 upgrade to increase sword size and damage. In a real
     * implementation you would apply multipliers to hitboxes and damage here.
     */
    override fun upgradePath2() {
        super.upgradePath2()
        // Additional sword‑specific effects could be placed here.
    }
}

/**
 * Implementation of the Axe weapon. This weapon performs a sweeping area
 * attack around the player and has upgrades for increasing area/damage and
 * improving lifesteal and attack speed.
 */
class Axe : Weapon() {
    override val type: WeaponType = WeaponType.AXE

    /**
     * The base damage for the Axe before any upgrades (20 as per design).
     */
    override fun baseDamage(): Int = 20

    /**
     * Increase area and damage when upgrading path1. Override allows
     * implementation of specific behaviour if needed.
     */
    override fun upgradePath1() {
        super.upgradePath1()
        // Axe‑specific area/damage multiplier adjustments would go here.
    }

    /**
     * Increase lifesteal and improve rotation interval when upgrading path2.
     * In a real game this might alter a timer controlling the attack.
     */
    override fun upgradePath2() {
        super.upgradePath2()
        // Axe‑specific lifesteal/interval adjustments would go here.
    }
}

/**
 * Implementation of the Bow weapon. Fires a spread of projectiles with the
 * possibility of critical hits. Upgrades increase projectile count and
 * critical statistics.
 */
class Bow : Weapon() {
    override val type: WeaponType = WeaponType.BOW

    /**
     * The base damage for the Bow before any upgrades (10 as per design).
     */
    override fun baseDamage(): Int = 10

    override fun upgradePath1() {
        super.upgradePath1()
        // Increase projectile count and reduce firing interval here.
    }

    override fun upgradePath2() {
        super.upgradePath2()
        // Increase critical chance and damage multipliers here.
    }
}

/**
 * Implementation of the Talisman weapon. Fires homing projectiles that
 * explode on impact. Upgrades increase explosion area/damage and the number
 * of projectiles fired.
 */
class Talisman : Weapon() {
    override val type: WeaponType = WeaponType.TALISMAN

    /**
     * The base damage for the Talisman before any upgrades (15 as per design).
     */
    override fun baseDamage(): Int = 15

    override fun upgradePath1() {
        super.upgradePath1()
        // Increase explosion area/damage here.
    }

    override fun upgradePath2() {
        super.upgradePath2()
        // Increase the number of projectiles here.
    }
}

/**
 * Static definitions of upgrade descriptions for each weapon and path. These
 * descriptions are used when presenting upgrade options to the player.
 */
object WeaponUpgradeDescriptions {
    // Sword: path1 increases the number of spinning swords
    val swordPath1 = arrayOf(
        "검 3개",             // Level 1: 3 swords
        "검 5개",             // Level 2: 5 swords
        "검 7개"              // Level 3: 7 swords
    )

    // Sword: path2 increases sword size and damage
    val swordPath2 = arrayOf(
        "검 크기 및 피해량 1.2배",    // Level 1
        "검 크기 및 피해량 1.5배",    // Level 2
        "검 크기 및 피해량 2배"       // Level 3
    )

    // Axe: path1 increases area and damage
    val axePath1 = arrayOf(
        "도끼 범위 및 피해량 1.3배",   // Level 1
        "도끼 범위 및 피해량 1.6배",   // Level 2
        "도끼 범위 및 피해량 1.8배"    // Level 3
    )

    // Axe: path2 increases lifesteal and reduces attack interval
    val axePath2 = arrayOf(
        "생명력 흡수 2배, 발동 간격 1.2초",  // Level 1
        "생명력 흡수 2배, 발동 간격 0.9초",  // Level 2
        "생명력 흡수 2배, 발동 간격 0.5초"   // Level 3
    )

    // Bow: path1 increases projectile count and reduces firing interval
    val bowPath1 = arrayOf(
        "투사체 5개, 발사 간격 0.75초",  // Level 1
        "투사체 9개, 발사 간격 0.5초",   // Level 2
        "투사체 15개, 발사 간격 0.25초" // Level 3
    )

    // Bow: path2 increases critical chance and damage
    val bowPath2 = arrayOf(
        "치명타 35%, 피해량 450%",   // Level 1
        "치명타 60%, 피해량 700%",   // Level 2
        "치명타 100%, 피해량 900%"   // Level 3
    )

    // Talisman: path1 increases explosion area and damage
    val talismanPath1 = arrayOf(
        "폭발 범위 및 피해량 1.2배",   // Level 1
        "폭발 범위 및 피해량 1.6배",   // Level 2
        "폭발 범위 및 피해량 2배"      // Level 3
    )

    // Talisman: path2 increases the number of homing projectiles
    val talismanPath2 = arrayOf(
        "투사체 3개",            // Level 1
        "투사체 7개",            // Level 2
        "투사체 12개"            // Level 3
    )

    /**
     * Fetch an upgrade description string for the given weapon, path and current level.
     *
     * @param weapon the weapon type to look up
     * @param path which upgrade path (1 or 2)
     * @param currentLevel the player's current level of the path (0–3)
     * @return the description for the next upgrade level, or an empty string if maxed
     */
    fun nextDescription(weapon: WeaponType, path: Int, currentLevel: Int): String {
        return when (weapon) {
            WeaponType.SWORD -> if (path == 1) {
                swordPath1.getOrNull(currentLevel) ?: ""
            } else {
                swordPath2.getOrNull(currentLevel) ?: ""
            }
            WeaponType.AXE -> if (path == 1) {
                axePath1.getOrNull(currentLevel) ?: ""
            } else {
                axePath2.getOrNull(currentLevel) ?: ""
            }
            WeaponType.BOW -> if (path == 1) {
                bowPath1.getOrNull(currentLevel) ?: ""
            } else {
                bowPath2.getOrNull(currentLevel) ?: ""
            }
            WeaponType.TALISMAN -> if (path == 1) {
                talismanPath1.getOrNull(currentLevel) ?: ""
            } else {
                talismanPath2.getOrNull(currentLevel) ?: ""
            }
        }
    }
}

/**
 * Represents the player character. Tracks health, experience, level progression
 * and manages the player's arsenal of weapons. When the player gains enough
 * experience, they level up which increases their stats and presents upgrade
 * choices.
 */
class Player(initialWeapon: Weapon) {
    // A map storing all weapons acquired by the player. The key is the weapon
    // type and the value is the concrete instance. New weapons are added as
    // upgrades unlock them.
    private val weapons: MutableMap<WeaponType, Weapon> = mutableMapOf()

    // The currently equipped weapon type.
    var activeWeaponType: WeaponType = initialWeapon.type
        private set

    // Player's current level (starts at 1).
    var level: Int = 1
        private set

    // Maximum health increases over time. Begins at 15.
    var maxHealth: Int = 15
        private set

    // Current health cannot exceed maxHealth.
    var currentHealth: Int = maxHealth
        private set

    // Accumulated experience points towards the next level.
    var experience: Int = 0
        private set

    // Experience required to reach the next level. Starts at 200 and doubles
    // each time a level is gained.
    var expForNextLevel: Int = 200
        private set

    init {
        // Store the initial weapon in the weapons map.
        weapons[initialWeapon.type] = initialWeapon
    }

    /**
     * Retrieve the current active weapon instance. If the player does not yet
     * own this weapon it will be created at its base level.
     */
    private fun getActiveWeapon(): Weapon {
        return weapons.getOrPut(activeWeaponType) { createWeapon(activeWeaponType) }
    }

    /**
     * Helper to create a new weapon instance based on its type. Called when
     * acquiring a weapon that the player did not previously own.
     *
     * @param type the weapon type to instantiate
     * @return a new weapon object of the given type
     */
    private fun createWeapon(type: WeaponType): Weapon {
        return when (type) {
            WeaponType.SWORD -> Sword()
            WeaponType.AXE -> Axe()
            WeaponType.BOW -> Bow()
            WeaponType.TALISMAN -> Talisman()
        }
    }

    /**
     * Adds experience points to the player. When enough experience has been
     * accumulated to reach the next level, the player will level up one or
     * more times in sequence. Excess experience beyond the required amount
     * carries over towards subsequent levels.
     *
     * @param amount the amount of experience to add
     */
    fun addExperience(amount: Int) {
        experience += amount
        // Handle multiple level‑ups if large amounts of experience are awarded.
        while (experience >= expForNextLevel) {
            experience -= expForNextLevel
            levelUp()
        }
    }

    /**
     * Perform a level up. This increases the player's level, maximum health
     * and adjusts the required experience for the next level. It also heals
     * the player for 75% of the missing health. Weapon upgrades are not
     * automatically applied here; instead upgrade options should be generated
     * and presented to the user externally.
     */
    private fun levelUp() {
        level += 1
        // Increase maximum health by 25
        maxHealth += 25
        // Heal 75% of missing health
        val missing = maxHealth - currentHealth
        val healAmount = (missing * 0.75).toInt()
        currentHealth += healAmount
        // Ensure current health does not exceed new maximum
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth
        }
        // Double the experience required for the next level
        expForNextLevel *= 2
    }

    /**
     * Inflict damage to the player. Health cannot drop below zero.
     *
     * @param damage the amount of damage received
     */
    fun receiveDamage(damage: Int) {
        currentHealth = (currentHealth - damage).coerceAtLeast(0)
    }

    /**
     * Restore some health to the player. Health will not exceed maximum
     * allowable health.
     *
     * @param amount the amount of healing to apply
     */
    fun heal(amount: Int) {
        currentHealth = (currentHealth + amount).coerceAtMost(maxHealth)
    }

    /**
     * Change the active weapon to another type. If the player does not yet
     * have this weapon in their arsenal it will be created in its base form.
     *
     * @param type the weapon type to equip
     */
    fun changeActiveWeapon(type: WeaponType) {
        activeWeaponType = type
        // Ensure the weapon exists in the arsenal
        weapons.getOrPut(type) { createWeapon(type) }
    }

    /**
     * Placeholder attack method. In a full game implementation this would
     * delegate to the active weapon's attack logic (e.g. spawning projectiles
     * or rotating hitboxes). Keeping this function allows game systems to call
     * Player.attack() without knowing the details of each weapon.
     */
    fun attack() {
        // Attack logic would interact with the game world here.
        // For example: getActiveWeapon().performAttack()
    }

    /**
     * Generate a list of three upgrade options when the player levels up. The
     * selection uses weighted randomness: upgrade paths for the currently
     * equipped weapon have a combined 40% chance of appearing, while the
     * remaining options are drawn evenly from the other weapons' upgrade
     * paths. Only upgrade paths that have not reached their maximum level
     * (level < 3) are considered.
     *
     * @return a list of up to three unique upgrade options
     */
    fun generateUpgradeOptions(): List<UpgradeOption> {
        val possibleOptions = mutableListOf<UpgradeOption>()
        // Build a list of all available upgrade paths (that are not maxed).
        for (type in WeaponType.values()) {
            val weapon = weapons.getOrPut(type) { createWeapon(type) }
            // If the first path has not reached level 3, offer it.
            if (weapon.path1Level < 3) {
                val desc = WeaponUpgradeDescriptions.nextDescription(type, 1, weapon.path1Level)
                possibleOptions.add(UpgradeOption(type, 1, desc))
            }
            // If the second path has not reached level 3, offer it.
            if (weapon.path2Level < 3) {
                val desc = WeaponUpgradeDescriptions.nextDescription(type, 2, weapon.path2Level)
                possibleOptions.add(UpgradeOption(type, 2, desc))
            }
        }
        // If there are fewer than three possible options, return them all.
        if (possibleOptions.size <= 3) {
            return possibleOptions
        }
        // Select three unique upgrade options using weighted randomness.
        val selected = mutableListOf<UpgradeOption>()
        val tempPool = possibleOptions.toMutableList()
        repeat(3) {
            val option = selectWeightedUpgrade(tempPool, activeWeaponType)
            selected.add(option)
            tempPool.remove(option)
        }
        return selected
    }

    /**
     * Apply a chosen upgrade option to the appropriate weapon. This method
     * should be called after the player selects an upgrade from the list
     * provided by [generateUpgradeOptions]. It ensures that the weapon
     * exists and then upgrades the correct path.
     *
     * @param option the upgrade selected by the player
     */
    fun applyUpgrade(option: UpgradeOption) {
        val weapon = weapons.getOrPut(option.weapon) { createWeapon(option.weapon) }
        weapon.applyUpgrade(option)
    }

    /**
     * Select one upgrade option from a list using weighted probabilities. This
     * method gives preference to the currently equipped weapon's upgrade paths
     * so that approximately 40% of selections favour the active weapon. The
     * remainder of the probability mass is divided evenly across all other
     * available upgrade options.
     *
     * @param pool a list of possible upgrades to choose from
     * @param startingType the weapon type that should be favoured
     * @return a single chosen upgrade option
     */
    private fun selectWeightedUpgrade(pool: List<UpgradeOption>, startingType: WeaponType): UpgradeOption {
        // Count how many options belong to the starting weapon
        val startingOptions = pool.count { it.weapon == startingType }
        val totalOptions = pool.size
        // Assign weights: 40% distributed across starting options, remainder across others
        val weights = DoubleArray(totalOptions)
        val remainderWeight = 1.0 - 0.4
        val nonStartingCount = totalOptions - startingOptions
        for (i in pool.indices) {
            val opt = pool[i]
            weights[i] = if (opt.weapon == startingType) {
                if (startingOptions > 0) 0.4 / startingOptions else 0.0
            } else {
                if (nonStartingCount > 0) remainderWeight / nonStartingCount else 0.0
            }
        }
        return weightedRandom(pool, weights)
    }

    /**
     * Choose a random element from a list according to the supplied weights.
     * Each weight corresponds to the same index in the items list. The sum of
     * all weights should be 1.0 for proper probability distribution, but
     * arbitrary sums are also handled.
     *
     * @param items the list of items to choose from
     * @param weights the probability weights for each item
     * @return a randomly chosen item
     */
    private fun <T> weightedRandom(items: List<T>, weights: DoubleArray): T {
        val totalWeight = weights.sum()
        val r = Random.nextDouble() * totalWeight
        var cumulative = 0.0
        for (i in items.indices) {
            cumulative += weights[i]
            if (r <= cumulative) {
                return items[i]
            }
        }
        // Fallback: return the last item if rounding errors occur
        return items.last()
    }
}