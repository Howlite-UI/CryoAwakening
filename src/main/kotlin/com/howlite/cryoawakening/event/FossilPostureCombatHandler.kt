package com.howlite.cryoawakening.event

import com.howlite.cryoawakening.item.ModItems
import com.howlite.cryoawakening.item.ModItems.ArmorTier
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

/**
 * Gestionnaire d'événement de combat pour la mécanique "Posture Fossile" (Full Set Bonus).
 *
 * Logique :
 * Lorsqu'un joueur accroupi portant un set complet subit des dégâts et que
 * son plastron n'est pas en cooldown :
 * 1. Les dégâts sont annulés à 100% (comportement bouclier).
 * 2. Un cooldown de 60 ticks (3s) est appliqué sur le plastron.
 * 3. Des sons de parade/ossement sont joués.
 * 4. Des effets spéciaux se déclenchent selon le tier :
 *    - FOSSILIZED (Tier 1) : Parade basique.
 *    - PRIMORDIAL (Tier 2) : Lenteur II appliquée à l'attaquant pendant 3 secondes.
 *    - APEX_GLACIAL (Tier 3) :
 *        * Onde de choc répulsive (AoE Knockback 5 blocs sur les ennemis).
 *        * Barrière de renvoi façon Z de Mel (League of Legends) : intercepte et renvoie tous les projectiles
 *          vers l'attaquant à pleine vitesse avec effets résonnants et traînée de particules.
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

            // B. Jouer les sons combinés de base (Shield Block + Skeleton Hurt)
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
                    val box = AABB.ofSize(player.position(), APEX_AOE_RADIUS * 2, APEX_AOE_RADIUS * 2, APEX_AOE_RADIUS * 2)

                    // 1. Onde de choc répulsive AoE sur les entités hostiles dans un rayon de 5 blocs
                    val hostiles = level.getEntitiesOfClass(LivingEntity::class.java, box) { target ->
                        (target is Enemy) && (target != player) && target.isAlive && (player.distanceTo(target) <= APEX_AOE_RADIUS)
                    }

                    for (target in hostiles) {
                        var pushDir = target.position().subtract(player.position())
                        if (pushDir.lengthSqr() < 1e-4) {
                            pushDir = Vec3(0.0, 0.0, 1.0)
                        }
                        val normalized = pushDir.normalize()
                        target.deltaMovement = Vec3(normalized.x * 1.65, 0.45, normalized.z * 1.65)
                        target.hurtMarked = true
                    }

                    // 2. Mécanique de Renvoi de Projectiles (Style Z de Mel - League of Legends)
                    val incomingDirect = source.directEntity
                    val nearbyProjectiles = level.getEntitiesOfClass(Projectile::class.java, box) { proj ->
                        proj.isAlive && proj.owner != player
                    }.toMutableList()

                    if (incomingDirect is Projectile && !nearbyProjectiles.contains(incomingDirect)) {
                        nearbyProjectiles.add(incomingDirect)
                    }

                    var reflectedAny = false
                    for (proj in nearbyProjectiles) {
                        reflectedAny = true
                        val originalShooter = source.entity ?: proj.owner

                        // Calcul de la cible du renvoi (la tête du tireur s'il est vivant, sinon vers la ligne de visée du joueur)
                        val targetPos = if (originalShooter != null && originalShooter.isAlive) {
                            originalShooter.eyePosition
                        } else {
                            proj.position().add(player.lookAngle.scale(15.0))
                        }

                        var dir = targetPos.subtract(proj.position())
                        if (dir.lengthSqr() < 1e-4) {
                            dir = player.lookAngle
                        }

                        // Vitesse amplifiée pour punir le tireur
                        val currentSpeed = proj.deltaMovement.length()
                        val reflectedSpeed = max(currentSpeed * 1.35, 1.85)
                        val normalizedDir = dir.normalize()
                        val reflectedVelocity = normalizedDir.scale(reflectedSpeed)

                        // Redirection et réassignation du projectile au joueur
                        proj.deltaMovement = reflectedVelocity
                        proj.owner = player
                        proj.hurtMarked = true

                        // Effets de traînée de renvoi de projectile
                        val dist = min(dir.length(), 14.0)
                        val steps = (dist * 2.5).toInt().coerceAtLeast(4)
                        for (i in 0 until steps) {
                            val progress = i.toDouble() / steps.toDouble()
                            val px = proj.x + dir.x * progress
                            val py = proj.y + dir.y * progress
                            val pz = proj.z + dir.z * progress
                            level.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01)
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0)
                        }

                        // Son d'impact et de résonance du renvoi cristallin
                        level.playSound(
                            null,
                            proj.x, proj.y, proj.z,
                            SoundEvents.AMETHYST_BLOCK_RESONATE,
                            SoundSource.PLAYERS,
                            1.2f,
                            1.6f
                        )
                        level.playSound(
                            null,
                            proj.x, proj.y, proj.z,
                            SoundEvents.PLAYER_ATTACK_SWEEP,
                            SoundSource.PLAYERS,
                            1.0f,
                            1.4f
                        )
                    }

                    if (reflectedAny) {
                        level.playSound(
                            null,
                            player.x, player.y, player.z,
                            SoundEvents.SHIELD_BLOCK,
                            SoundSource.PLAYERS,
                            1.4f,
                            1.75f
                        )
                    }

                    // 3. Anneau circulaire de particules de flocons de neige (ParticleTypes.SNOWFLAKE)
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
                        if (i % 3 == 0) {
                            level.sendParticles(
                                ParticleTypes.ELECTRIC_SPARK,
                                px, player.y + 0.25, pz,
                                1,
                                0.0, 0.05, 0.0,
                                0.02
                            )
                        }
                    }

                    // 4. Explosion centrale de flocons et étincelles autour du joueur
                    level.sendParticles(
                        ParticleTypes.SNOWFLAKE,
                        player.x, player.y + 1.0, player.z,
                        35,
                        1.2, 0.5, 1.2,
                        0.08
                    )
                    level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        player.x, player.y + 1.0, player.z,
                        15,
                        0.8, 0.4, 0.8,
                        0.05
                    )
                }
            }

            // D. Annuler complètement les dégâts subis
            return@register false
        }
    }
}
