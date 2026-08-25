package com.howlite.cryoawakening.energy

import net.minecraft.core.Direction

/**
 * Interface pour les blocs et BlockEntities capables de stocker, recevoir ou émettre du Vent.
 */
interface IWindHolder {
    fun getWindStorage(side: Direction?): WindStorage?
}
