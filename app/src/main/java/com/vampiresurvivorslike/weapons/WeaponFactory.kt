package com.vampiresurvivorslike.weapons

import android.content.Context

object WeaponFactory {

    // [수정] context 인자 추가 -> 각 무기 생성자에 전달
    fun createWeapon(context: Context, type: String): Weapon {
        return when (type.lowercase()) {
            "sword" -> Sword(context)
            "axe" -> Axe(context)
            "bow" -> Bow(context)
            "talisman" -> Talisman(context)
            else -> Sword(context)
        }
    }

    // [수정] context 인자 추가
    fun createDefault(context: Context): Weapon = Sword(context)
}