package com.vampiresurvivorslike.weapons

object WeaponFactory {
    fun createWeapon(type: String): Weapon {
        return when (type.lowercase()) {
            "sword" -> Sword()
            "axe" -> Axe()
            "bow" -> Bow()
            "talisman" -> Talisman()
            else -> Sword()
        }
    }
    fun createDefault(): Weapon = Sword()
}
