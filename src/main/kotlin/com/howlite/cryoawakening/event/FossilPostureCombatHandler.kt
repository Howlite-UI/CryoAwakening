package com.howlite.cryoawakening.event

import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.item.ModItems.ArmorTier
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Gestionnaire d'événement de combat pour la mécanique "Posture Fossile" (Full Set Bonus).
 *
 * Logique :
 * Lorsqu'un joueur accroupi portant un set complet subit des dégâts physiques et que
 * son plastron n'est pas en cooldown :
 * 1. Les dégâts sont annulés à 100% (comportement bouclier).
 * 2. Un cooldown de 60 ticks (3s) est appliqué sur le plastron.
 * 3. Des sons de blocage/ossement sont joués.
 * 4. Des effets spéciaux se déclenchent selon le tier :
 *    - FOSSILIZED (Tier 1) : Parade basique.
 *    - PRIMORDIAL (Tier 2) : Lenteur II appliquée à l'attaquant pendant 3 secondes.
 *    - APEX_GLACIAL (Tier 3) : Onde de choc répulsive (AoE Knockback) sur les ennemis à 5 blocs + particules de flocons de neige.
 */
object FossilPostureCombatHandler {

    private const val COOLDOWN_TICKS: Int = 60 // 3 secondes (20 ticks/sec * 3)
    private const val APEX_AOE_RADIUS: Double = 5.0

    fun register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            // 1. Vérification : la cible doit être un joueur côté serveur
            val player = entity as? ServerPlayer ?: return@register true

            // Si les dégâts traversent l'invulnérabilité (ex: vide, /kill), ne pas bloquer
            if (source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return@register true
            }

            // 2. Vérification : le joueur doit être accroupi (sneak)
            if (!player.isShiftKeyDown && !player.isCrouching) {
                return@register true
            }

            // 3. Vérification : le joueur doit porter un set complet de l'une des 3 armures
            val tier = ModItems.getEquippedFullSetTier(player) ?: return@register true

            // 4. Vérification : le plastron ne doit pas être en cooldown
            val chestStack = player.getItemBySlot(EquipmentSlot.CHEST)
            if (chestStack.isEmpty || player.cooldowns.isOnCooldown(chestStack)) {
                return@register true
            }

            // --- CONDITIONS REMPLIES : DÉCLENCHEMENT DE LA POSTURE FOSSILE ---

            // A. Appliquer le cooldown de 3 secondes (60 ticks) sur le plastron
            player.cooldowns.addCooldown(chestStack, COOLDOWN_TICKS)

            val level = player.level()

            // B. Jouer les sons combinés (Shield Block + Skeleton Hurt)
            level.playSound(
                null,
                player.x, player.y, player.z,
                SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
            )
            level.playSound(
                null,
                player.x, player.y, player.z,
                SoundEvents.SKELETON_HURT,
                SoundSource.PLAYERS,
                0.85f,
                0.85f
            )

            // C. Effets spécifiques par Tier
            when (tier) {
                ArmorTier.FOSSILIZED -> {
                    // Tier 1 : Blocage pur, aucun effet additionnel
                }

                ArmorTier.PRIMORDIAL -> {
                    // Tier 2 : Lenteur II (amplificateur 1) pendant 3 secondes (60 ticks) sur l'attaquant
                    val attacker = source.entity ?: source.directEntity
                    if (attacker is LivingEntity) {
                        attacker.addEffect(
                            MobEffectInstance(MobEffects.SLOWNESS, COOLDOWN_TICKS, 1, false, true, true)
                        )
                    }
                }

                ArmorTier.APEX_GLACIAL -> {
                    // Tier 3 : Onde de choc répulsive AoE dans un rayon de 5 blocs
                    val box = AABB.ofSize(player.position(), APEX_AOE_RADIUS * 2, APEX_AOE_RADIUS * 2, APEX_AOE_RADIUS * 2)
                    val hostiles = level.getEntitiesOfClass(LivingEntity::class.java, box) { target ->
                        (target is Enemy) && (target != player) && target.isAlive && (player.distanceTo(target) <= APEX_AOE_RADIUS)
                    }

                    for (target in hostiles) {
                        var pushDir = target.position().subtract(player.position())
                        if (pushDir.lengthSqr() < 1e-4) {
                            pushDir = Vec3(0.0, 0.0, 1.0)
                        }
                        val normalized = pushDir.normalize()
                        // Projection puissante vers l'arrière avec une légère élévation verticale
                        target.deltaMovement = Vec3(normalized.x * 1.65, 0.45, normalized.z * 1.65)
                        target.hurtMarked = true
                    }

                    // Anneau circulaire de particules de flocons de neige (ParticleTypes.SNOWFLAKE)
                    val particleCount = 48
                    for (i in 0 until particleCount) {
                        val angle = 2.0 * Math.PI * i / particleCount
                        val px = player.x + APEX_AOE_RADIUS * Math.cos(angle)
                        val pz = player.z + APEX_AOE_RADIUS * Math.sin(angle)
                        level.sendParticles(
                            ParticleTypes.SNOWFLAKE,
                            px, player.y + 0.25, pz,
                            2,
                            0.05, 0.1, 0.05,
                            0.02
                        )
                    }

                    // Explosion centrale de flocons autour du joueur
                    level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        player.x, player.y + 1.0, player.z,
                        30,
                        1.2, 0.5, 1.2,
                        0.08
                    )
                }
            }

            // D. Annuler complètement les dégâts subis
            return@register false
        }
    }
}
