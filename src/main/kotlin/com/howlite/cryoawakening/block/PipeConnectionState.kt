package com.howlite.cryoawakening.block

import net.minecraft.util.StringRepresentable

/**
 * État de connexion d'une branche de tuyau (Gale Pipe) :
 * - NONE : Déconnecté (aucun bras affiché)
 * - NORMAL : Connexion standard (affiche gale_pipe_arm)
 * - EXTRACT : Extraction active depuis une machine (affiche gale_pipe_out avec bride)
 */
enum class PipeConnectionState(val id: String) : StringRepresentable {
    NONE("none"),
    NORMAL("normal"),
    EXTRACT("extract");

    override fun getSerializedName(): String = id

    fun isConnected(): Boolean = this != NONE
}
