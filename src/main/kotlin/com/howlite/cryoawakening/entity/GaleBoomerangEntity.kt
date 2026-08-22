package com.howlite.cryoawakening.entity

import com.geckolib.animatable.GeoEntity
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.util.GeckoLibUtil
import com.howlite.cryoawakening.enchantment.ModEnchantments
import net.minecraft.core.BlockPos
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * GaleBoomerangEntity
 *
 * Entité projectile complexe représentant le Gale Boomerang (Zelda: Twilight Princess).
 * Gère une machine à 3 états principaux (LAUNCHED, HOVERING, RETURNING)
 * ainsi que les 8 enchantements personnalisés (Heavyweight, Zephyr, Ricochet, Soar, Frostwind, Orbit, Retrieval, Gale Vortex).
 */
class GaleBoomerangEntity(
    entityType: EntityType<out PathfinderMob>,
    level: Level
) : PathfinderMob(entityType, level), GeoEntity {

    private val geoCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    enum class BoomerangState(val id: Int) {
        LAUNCHED(0),
        HOVERING(1),
        RETURNING(2);

        companion object {
            fun fromId(id: Int): BoomerangState = entries.firstOrNull { it.id == id } ?: LAUNCHED
        }
    }

    companion object {
        val BOOMERANG_STATE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GaleBoomerangEntity::class.java, EntityDataSerializers.INT)

        val FLIGHT_TICKS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GaleBoomerangEntity::class.java, EntityDataSerializers.INT)

        val HOVER_TICKS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GaleBoomerangEntity::class.java, EntityDataSerializers.INT)

        val CARRIED_ITEM_COUNT: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(GaleBoomerangEntity::class.java, EntityDataSerializers.INT)

        fun createAttributes(): AttributeSupplier.Builder {
            return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FLYING_SPEED, 1.5)
        }
    }

    // --- Données de vol & Lancer ---
    private var throwerUuid: UUID? = null
    private var throwForce: Float = 1.0f
    private var launchOrigin: Vec3 = Vec3.ZERO
    private var launchDirection: Vec3 = Vec3.ZERO
    private var usedHand: InteractionHand = InteractionHand.MAIN_HAND

    // --- Enchantements du Boomerang ---
    var boomerangStack: ItemStack = ItemStack.EMPTY
    var heavyweightLevel: Int = 0
    var zephyrLevel: Int = 0
    var ricochetLevel: Int = 0
    var soarLevel: Int = 0
    var frostwindLevel: Int = 0
    var orbitLevel: Int = 0
    var retrievalLevel: Int = 0
    var galeVortexLevel: Int = 0

    // --- Variables d'états dynamiques ---
    private var currentTargetIndex: Int = 0
    private val targetEntityIds = ArrayList<Int>()
    private val targetPositions = ArrayList<Vec3>()

    private var hoverCenter: Vec3 = Vec3.ZERO
    private var isFreeFlight: Boolean = true
    private var maxFreeFlightTicks: Int = 22

    // Items et XP ramassés et transportés vers le joueur
    val carriedItems = ArrayList<ItemStack>()
    var carriedXp: Int = 0

    // Sous-état Orbit
    private var isOrbiting: Boolean = false
    private var orbitTicks: Int = 0
    private var hasCompletedOrbit: Boolean = false

    // Entités déjà touchées
    private val hitEntities = HashSet<UUID>()

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(BOOMERANG_STATE, BoomerangState.LAUNCHED.id)
        builder.define(FLIGHT_TICKS, 0)
        builder.define(HOVER_TICKS, 0)
        builder.define(CARRIED_ITEM_COUNT, 0)
    }

    override fun registerGoals() {
        // Pas d'IA classique : mouvement contrôlé procéduralement
    }

    /**
     * Initialise le lancer du Gale Boomerang depuis le joueur avec ses cibles et enchantements.
     */
    fun setupThrow(
        thrower: Player,
        force: Float,
        targetIds: List<Int>,
        hand: InteractionHand,
        stack: ItemStack = ItemStack.EMPTY
    ) {
        this.throwerUuid = thrower.uuid
        this.throwForce = force.coerceIn(0.5f, 2.0f)
        this.usedHand = hand
        this.boomerangStack = stack.copy()
        this.boomerangState = BoomerangState.LAUNCHED
        this.flightTicks = 0
        this.hoverTicks = 0
        this.currentTargetIndex = 0
        this.launchOrigin = thrower.eyePosition
        this.launchDirection = thrower.lookAngle.normalize()
        this.setNoGravity(true)
        this.noPhysics = true

        // Lecture des enchantements depuis l'item
        this.heavyweightLevel = ModEnchantments.getLevel(stack, ModEnchantments.HEAVYWEIGHT, level())
        this.zephyrLevel = ModEnchantments.getLevel(stack, ModEnchantments.ZEPHYR, level())
        this.ricochetLevel = ModEnchantments.getLevel(stack, ModEnchantments.RICOCHET, level())
        this.soarLevel = ModEnchantments.getLevel(stack, ModEnchantments.SOAR, level())
        this.frostwindLevel = ModEnchantments.getLevel(stack, ModEnchantments.FROSTWIND, level())
        this.orbitLevel = ModEnchantments.getLevel(stack, ModEnchantments.ORBIT, level())
        this.retrievalLevel = ModEnchantments.getLevel(stack, ModEnchantments.RETRIEVAL, level())
        this.galeVortexLevel = ModEnchantments.getLevel(stack, ModEnchantments.GALE_VORTEX, level())
        val hawkeyeLevel = ModEnchantments.getLevel(stack, ModEnchantments.HAWKEYE, level())

        this.targetEntityIds.clear()
        this.targetPositions.clear()
        this.hitEntities.clear()
        this.carriedItems.clear()
        this.carriedXp = 0
        this.isOrbiting = false
        this.orbitTicks = 0
        this.hasCompletedOrbit = false

        // Filtrer et mémoriser les positions initiales des entités ciblées
        for (id in targetIds) {
            val entity = level().getEntity(id)
            if (entity != null && entity != thrower && entity.isAlive) {
                targetEntityIds.add(id)
                targetPositions.add(entity.position().add(0.0, entity.bbHeight * 0.5, 0.0))
            }
        }

        this.isFreeFlight = targetEntityIds.isEmpty()
        this.maxFreeFlightTicks = (16 + (force * 10) + (zephyrLevel * 4) + (hawkeyeLevel * 6)).toInt()

        // Calcul de la vitesse initiale selon Zephyr (+30%/niv) et Heavyweight (-30%)
        val speedMod = (1.0f + (zephyrLevel * 0.30f) - (if (heavyweightLevel > 0) 0.30f else 0.0f)).coerceAtLeast(0.35f)
        val initialSpeed = 0.70 * speedMod * force
        this.deltaMovement = if (targetPositions.isNotEmpty()) {
            targetPositions[0].subtract(position()).normalize().scale(initialSpeed)
        } else {
            launchDirection.scale(initialSpeed)
        }

        // Son de lancer
        level().playSound(
            null,
            thrower.blockPosition(),
            SoundEvents.TRIDENT_THROW.value(),
            SoundSource.PLAYERS,
            1.0f,
            1.1f + (force * 0.2f)
        )
    }

    override fun tick() {
        super.tick()
        flightTicks++

        // Gestion côté client : Particules & Sons
        if (level().isClientSide) {
            spawnClientVisuals()
            return
        }

        val serverLevel = level() as? ServerLevel ?: return

        // Enchantement Retrieval : ramassage continu de tous les items et XP croisés en vol
        if (retrievalLevel > 0) {
            collectAllNearbyItemsAndXp(serverLevel)
        }

        // Gestion des phases de vol
        when (boomerangState) {
            BoomerangState.LAUNCHED -> tickLaunched(serverLevel)
            BoomerangState.HOVERING -> tickHovering(serverLevel)
            BoomerangState.RETURNING -> tickReturning(serverLevel)
        }

        // Déplacement physique de l'entité
        if (!isOrbiting) {
            setPos(x + deltaMovement.x, y + deltaMovement.y, z + deltaMovement.z)
        }
    }

    // =========================================================================
    // ÉTAT 1 : LAUNCHED (Vol direct vers les cibles marquées ou en tir libre)
    // =========================================================================
    private fun tickLaunched(serverLevel: ServerLevel) {
        val speedMod = (1.0f + (zephyrLevel * 0.30f) - (if (heavyweightLevel > 0) 0.30f else 0.0f)).coerceAtLeast(0.35f)
        val speed = 0.70 * speedMod * throwForce

        // Enchantement Frostwind : congélation de l'eau sous la trajectoire
        if (frostwindLevel > 0) {
            freezeWaterAt(serverLevel, blockPosition())
            freezeWaterAt(serverLevel, blockPosition().below())
        }

        if (!isFreeFlight && currentTargetIndex < targetEntityIds.size) {
            val targetId = targetEntityIds[currentTargetIndex]
            val targetEntity = serverLevel.getEntity(targetId)

            val isTargetInvalid = targetEntity == null || !targetEntity.isAlive
            val targetPos = if (targetEntity != null && targetEntity.isAlive) {
                targetEntity.position().add(0.0, targetEntity.bbHeight * 0.5, 0.0)
            } else if (currentTargetIndex < targetPositions.size) {
                targetPositions[currentTargetIndex]
            } else {
                position()
            }

            val toTarget = targetPos.subtract(position())
            val dist = toTarget.length()

            // Vérification d'atteinte de la cible
            val reachedTarget = dist <= (speed * 1.6) || dist <= 1.4 ||
                    (targetEntity != null && boundingBox.inflate(0.8).intersects(targetEntity.boundingBox)) ||
                    (isTargetInvalid && dist <= 2.2)

            if (reachedTarget) {
                // Frapper la créature ciblée ou ramasser l'item spécifiquement ciblé
                if (targetEntity is LivingEntity && targetEntity != getThrower()) {
                    strikeEntity(serverLevel, targetEntity)
                } else if (targetEntity is ItemEntity) {
                    pickupItem(targetEntity)
                }

                currentTargetIndex++
                if (currentTargetIndex >= targetEntityIds.size) {
                    // Toutes les cibles ont été atteintes : stase ou retour immédiat
                    enterHoverState(targetPos)
                    return
                } else {
                    // Se réorienter directement vers la cible suivante
                    val nextTargetId = targetEntityIds[currentTargetIndex]
                    val nextEntity = serverLevel.getEntity(nextTargetId)
                    val nextPos = if (nextEntity != null && nextEntity.isAlive) {
                        nextEntity.position().add(0.0, nextEntity.bbHeight * 0.5, 0.0)
                    } else if (currentTargetIndex < targetPositions.size) {
                        targetPositions[currentTargetIndex]
                    } else {
                        targetPos
                    }
                    val toNext = nextPos.subtract(position())
                    deltaMovement = if (toNext.length() > 0.01) toNext.normalize().scale(speed) else deltaMovement
                }
            } else {
                // Vol direct vers la cible
                deltaMovement = toTarget.normalize().scale(speed)
            }
        } else if (isFreeFlight) {
            // --- Tir libre en courbe balistique Zelda ---
            val progress = (flightTicks.toFloat() / maxFreeFlightTicks).coerceIn(0.0f, 1.0f)

            // Détection des entités sur la trajectoire libre
            val hitBox = boundingBox.inflate(0.8)
            val nearby = serverLevel.getEntitiesOfClass(LivingEntity::class.java, hitBox) {
                it != this && it != getThrower() && it.isAlive && !hitEntities.contains(it.uuid)
            }
            for (victim in nearby) {
                hitEntities.add(victim.uuid)
                strikeEntity(serverLevel, victim)
            }

            // Courbure latérale aérodynamique
            if (flightTicks >= maxFreeFlightTicks) {
                enterHoverState(position())
                return
            } else {
                val sideVec = Vec3(-launchDirection.z, 0.0, launchDirection.x).normalize()
                val curveAmt = sin(progress * Math.PI) * 0.35
                val forwardAmt = cos(progress * Math.PI * 0.5)
                val targetDir = launchDirection.scale(forwardAmt).add(sideVec.scale(curveAmt)).normalize()
                deltaMovement = targetDir.scale(speed)
            }
        } else {
            enterHoverState(position())
            return
        }

        // Détection de collision contre un mur / bloc solide
        val nextPos = position().add(deltaMovement)
        val clip = serverLevel.clip(ClipContext(position(), nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
        if (clip.type == HitResult.Type.BLOCK) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0f, 1.6f)

            if (!isFreeFlight && currentTargetIndex + 1 < targetEntityIds.size) {
                currentTargetIndex++
                val nextTargetId = targetEntityIds[currentTargetIndex]
                val nextEntity = serverLevel.getEntity(nextTargetId)
                val nextPosTarget = if (nextEntity != null && nextEntity.isAlive) nextEntity.position().add(0.0, nextEntity.bbHeight * 0.5, 0.0) else targetPositions[currentTargetIndex]
                deltaMovement = nextPosTarget.subtract(position()).normalize().scale(speed)
            } else {
                enterHoverState(clip.location.subtract(deltaMovement.normalize().scale(0.5)))
            }
        }
    }

    // =========================================================================
    // ÉTAT 2 : HOVERING (Stase d'apex, Gale Vortex ou Frostwind)
    // =========================================================================
    private fun enterHoverState(pos: Vec3) {
        this.boomerangState = BoomerangState.HOVERING
        this.hoverTicks = 0
        this.hoverCenter = pos
        this.deltaMovement = Vec3.ZERO

        level().playSound(
            null,
            blockPosition(),
            SoundEvents.TRIDENT_RETURN,
            SoundSource.PLAYERS,
            0.8f,
            1.4f
        )
    }

    private fun tickHovering(serverLevel: ServerLevel) {
        hoverTicks++

        val maxApexTicks = when {
            galeVortexLevel > 0 -> 18 + (galeVortexLevel * 10)
            frostwindLevel > 0 -> 12
            else -> 4
        }

        // Enchantement Gale Vortex : tornade équilibrée
        if (galeVortexLevel > 0) {
            val vortexRadius = 4.0 + (galeVortexLevel * 1.5)
            val center = hoverCenter

            // 1. Aspiration fluide des items au sol
            val items = serverLevel.getEntitiesOfClass(
                ItemEntity::class.java,
                AABB(center.x - vortexRadius, center.y - 2.5, center.z - vortexRadius, center.x + vortexRadius, center.y + 3.5, center.z + vortexRadius)
            ) { it.isAlive }
            for (item in items) {
                val toCenter = center.subtract(item.position())
                val d = toCenter.length()
                if (d < 1.3) {
                    pickupItem(item)
                } else {
                    val pull = toCenter.normalize().scale(0.18 + (galeVortexLevel * 0.05))
                    item.deltaMovement = item.deltaMovement.scale(0.75).add(pull.x, 0.06, pull.z)
                    item.hurtMarked = true
                }
            }

            // 2. Aspiration et vortex tourbillonnant des monstres
            val victims = serverLevel.getEntitiesOfClass(
                LivingEntity::class.java,
                AABB(center.x - vortexRadius, center.y - 2.0, center.z - vortexRadius, center.x + vortexRadius, center.y + 3.5, center.z + vortexRadius)
            ) { it != this && it != getThrower() && it.isAlive }

            for (victim in victims) {
                val toCenter = center.subtract(victim.position())
                val d = toCenter.length()
                if (d > 0.4) {
                    val pullForce = 0.14 + (galeVortexLevel * 0.05)
                    val pull = toCenter.normalize().scale(pullForce)
                    victim.deltaMovement = victim.deltaMovement.scale(0.70).add(pull.x, 0.05, pull.z)
                    victim.hurtMarked = true

                    // Dégâts de tornade toutes les 10 ticks
                    if (hoverTicks % 10 == 0) {
                        val thrower = getThrower()
                        val src = if (thrower != null) serverLevel.damageSources().playerAttack(thrower) else serverLevel.damageSources().magic()
                        victim.hurtServer(serverLevel, src, 1.0f + (galeVortexLevel * 0.75f))
                    }
                }
            }

            // 3. Aspiration des projectiles / flèches
            val projectiles = serverLevel.getEntitiesOfClass(
                Projectile::class.java,
                AABB(center.x - vortexRadius, center.y - 2.5, center.z - vortexRadius, center.x + vortexRadius, center.y + 2.5, center.z + vortexRadius)
            ) { it != this }
            for (proj in projectiles) {
                val toCenter = center.subtract(proj.position())
                proj.deltaMovement = proj.deltaMovement.scale(0.75).add(toCenter.normalize().scale(0.15))
            }

            // 4. Extinction des flammes
            if (hoverTicks % 4 == 0) {
                extinguishFiresInRadius(serverLevel, center, vortexRadius.toInt())
            }

            // Son de vent continu
            if (hoverTicks % 8 == 1) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.1f, 1.0f + (galeVortexLevel * 0.15f))
            }
        }

        // Enchantement Frostwind : vague de gel à l'apex
        if (frostwindLevel > 0 && hoverTicks == 1) {
            freezeWaterInRadius(serverLevel, hoverCenter, 2 + frostwindLevel)
        }

        // Fin de l'apex : amorce du retour vers le joueur
        if (hoverTicks >= maxApexTicks) {
            boomerangState = BoomerangState.RETURNING
            val thrower = getThrower()
            val destPos = if (thrower != null && thrower.isAlive && thrower.level() == level()) {
                thrower.position().add(0.0, thrower.eyeHeight * 0.6, 0.0)
            } else {
                launchOrigin
            }
            val toPlayer = destPos.subtract(position())
            val speedMod = (1.0f + (zephyrLevel * 0.30f) - (if (heavyweightLevel > 0) 0.30f else 0.0f)).coerceAtLeast(0.35f)
            val returnSpeed = 0.85 * speedMod * throwForce
            deltaMovement = if (toPlayer.length() > 0.01) toPlayer.normalize().scale(returnSpeed) else Vec3.ZERO
        }
    }

    // =========================================================================
    // ÉTAT 3 : RETURNING (Retour guidé, Orbit protecteur et Mobilité Soar)
    // =========================================================================
    private fun tickReturning(serverLevel: ServerLevel) {
        val thrower = getThrower()

        val destPos = if (thrower != null && thrower.isAlive && thrower.level() == level()) {
            thrower.position().add(0.0, thrower.eyeHeight * 0.6, 0.0)
        } else {
            launchOrigin
        }

        val toPlayer = destPos.subtract(position())
        val dist = toPlayer.length()
        val speedMod = (1.0f + (zephyrLevel * 0.30f) - (if (heavyweightLevel > 0) 0.30f else 0.0f)).coerceAtLeast(0.35f)
        val returnSpeed = 0.85 * speedMod * throwForce

        // Enchantement Orbit : nombre de tours proportionnel au niveau (Niv 1: ~2 tours, Niv 2: ~4 tours, Niv 3: ~6 tours)
        if (orbitLevel > 0 && !hasCompletedOrbit && thrower != null && thrower.isAlive) {
            if (dist <= 3.2 || isOrbiting) {
                if (!isOrbiting) {
                    isOrbiting = true
                    orbitTicks = 0
                    hitEntities.clear()
                }

                orbitTicks++

                // Réinitialiser les entités touchables toutes les 10 ticks (environ 1 demi-tour)
                if (orbitTicks % 10 == 0) {
                    hitEntities.clear()
                }

                val angle = orbitTicks * 0.32
                val orbitRadius = 2.2
                val orbitPos = thrower.position().add(
                    cos(angle) * orbitRadius,
                    thrower.eyeHeight * 0.5,
                    sin(angle) * orbitRadius
                )
                setPos(orbitPos.x, orbitPos.y, orbitPos.z)
                deltaMovement = Vec3.ZERO

                // Frapper les monstres proches en orbite avec dégâts et recul
                val nearbyMonsters = serverLevel.getEntitiesOfClass(LivingEntity::class.java, boundingBox.inflate(1.2)) {
                    it != this && it != thrower && it.isAlive && !hitEntities.contains(it.uuid)
                }
                for (monster in nearbyMonsters) {
                    hitEntities.add(monster.uuid)
                    strikeEntity(serverLevel, monster)
                }

                // Fin d'orbite selon le niveau (Niv 1 = 38 ticks, Niv 2 = 76 ticks, Niv 3 = 114 ticks)
                val maxOrbitTicks = (orbitLevel.coerceIn(1, 3) * 38)
                if (orbitTicks >= maxOrbitTicks) {
                    isOrbiting = false
                    hasCompletedOrbit = true
                    catchBoomerang(serverLevel, thrower)
                    return
                }
                return
            }
        }

        // Détection d'interception du joueur (< 2.2 blocs)
        if (dist <= 2.2 || dist <= (returnSpeed * 1.8) || (thrower != null && boundingBox.inflate(1.2).intersects(thrower.boundingBox))) {
            catchBoomerang(serverLevel, thrower)
            return
        }

        // Homing direct vers le joueur
        deltaMovement = toPlayer.normalize().scale(returnSpeed)

        // Timeout de sécurité (10 secondes)
        if (flightTicks > 200) {
            catchBoomerang(serverLevel, thrower)
        }
    }

    /**
     * Frappe une entité et applique les enchantements de combat et contrôle.
     */
    private fun strikeEntity(serverLevel: ServerLevel, target: LivingEntity) {
        val thrower = getThrower()
        val dmgSource = if (thrower != null) {
            serverLevel.damageSources().playerAttack(thrower)
        } else {
            serverLevel.damageSources().magic()
        }

        // Heavyweight : +25% de dégâts bruts par niveau
        val damageMultiplier = 1.0f + (heavyweightLevel * 0.25f)
        val baseDamage = 6.0f * damageMultiplier * throwForce
        target.hurtServer(serverLevel, dmgSource, baseDamage)

        // Heavyweight : fort recul + étourdissement (Slowness IV & Weakness) + brise-bouclier
        if (heavyweightLevel > 0) {
            val knock = deltaMovement.normalize().scale(0.70 + (heavyweightLevel * 0.20))
            target.push(knock.x, 0.20, knock.z)
            target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 30, 3))
            if (target is Player && target.isBlocking) {
                val shieldStack = target.useItem
                target.cooldowns.addCooldown(shieldStack, 60)
                target.stopUsingItem()
            }
        } else {
            val knockDir = deltaMovement.normalize()
            target.push(knockDir.x * 0.45, 0.20, knockDir.z * 0.45)
        }

        // Soar : projection verticale des ennemis dans les airs
        if (soarLevel > 0) {
            val verticalBoost = 0.45 + (soarLevel * 0.25)
            target.push(0.0, verticalBoost, 0.0)
        }

        // Frostwind : gelure + Slowness II + gel d'eau
        if (frostwindLevel > 0) {
            target.ticksFrozen = (target.ticksFrozen + (140 * frostwindLevel)).coerceAtMost(400)
            target.addEffect(MobEffectInstance(MobEffects.SLOWNESS, 60, 1))
            freezeWaterInRadius(serverLevel, target.position(), 1 + frostwindLevel)
        }

        target.hurtMarked = true

        // Retrieval : capture immédiate des loots si le monstre est éliminé par le coup
        if (retrievalLevel > 0) {
            collectAllNearbyItemsAndXp(serverLevel)
        }

        // Son d'impact
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.4f)
    }

    /**
     * Ramasse tous les items et orbes d'XP proches (Enchantement Retrieval).
     */
    private fun collectAllNearbyItemsAndXp(serverLevel: ServerLevel) {
        val radius = 3.5
        val nearbyItems = serverLevel.getEntitiesOfClass(ItemEntity::class.java, boundingBox.inflate(radius)) {
            it.isAlive
        }
        for (item in nearbyItems) {
            pickupItem(item)
        }

        val nearbyOrbs = serverLevel.getEntitiesOfClass(ExperienceOrb::class.java, boundingBox.inflate(radius)) {
            it.isAlive
        }
        for (orb in nearbyOrbs) {
            carriedXp += orb.value
            orb.discard()
            level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 1.5f)
        }
    }

    private fun pickupItem(itemEntity: ItemEntity) {
        val stack = itemEntity.item.copy()
        if (!stack.isEmpty) {
            carriedItems.add(stack)
            carriedItemCount = carriedItems.size

            level().playSound(
                null,
                blockPosition(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.9f,
                1.4f + (carriedItems.size * 0.1f)
            )
            itemEntity.discard()
        }
    }

    /**
     * Rendu des items, distribution de l'XP et impulsion de super-saut Soar sans dégâts de chute.
     */
    private fun catchBoomerang(serverLevel: ServerLevel, thrower: Player?) {
        if (thrower != null) {
            for (stack in carriedItems) {
                if (!thrower.inventory.add(stack)) {
                    thrower.drop(stack, false)
                }
            }
            if (carriedXp > 0) {
                thrower.giveExperiencePoints(carriedXp)
            }

            // Soar : super-saut joueur sans dégâts de chute
            if (soarLevel > 0 && (!thrower.onGround() || thrower.isFallFlying)) {
                val jumpBoost = 0.65 + (soarLevel * 0.25)
                thrower.push(0.0, jumpBoost, 0.0)
                thrower.resetFallDistance()
                thrower.fallDistance = 0.0
                thrower.addEffect(MobEffectInstance(MobEffects.SLOW_FALLING, 60 + (soarLevel * 20), 0, false, false, true))
                thrower.hurtMarked = true
                serverLevel.playSound(null, thrower.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0f, 1.4f)
            }

            serverLevel.playSound(null, thrower.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.2f, 1.1f)
            serverLevel.playSound(null, thrower.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.6f)
        } else {
            dropAllCarriedItems(serverLevel)
        }

        carriedItems.clear()
        carriedItemCount = 0
        carriedXp = 0
        discard()
    }

    private fun dropAllCarriedItems(serverLevel: ServerLevel) {
        for (stack in carriedItems) {
            spawnAtLocation(serverLevel, stack)
        }
        carriedItems.clear()
        carriedItemCount = 0
        carriedXp = 0
    }

    private fun freezeWaterAt(serverLevel: ServerLevel, pos: BlockPos) {
        val state = serverLevel.getBlockState(pos)
        // Vérifie si le bloc est de l'eau (source) et si l'air au-dessus permet de marcher dessus
        if (state.`is`(Blocks.WATER) && state.fluidState.isSource) {
            val frostedIceState = Blocks.FROSTED_ICE.defaultBlockState()
            serverLevel.setBlockAndUpdate(pos, frostedIceState)
            serverLevel.scheduleTick(pos, Blocks.FROSTED_ICE, net.minecraft.util.Mth.nextInt(serverLevel.random, 60, 120))
            serverLevel.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.4f, 1.6f)
        }
    }

    private fun freezeWaterInRadius(serverLevel: ServerLevel, center: Vec3, radius: Int) {
        val cx = center.x.toInt()
        val cy = center.y.toInt()
        val cz = center.z.toInt()

        for (bx in (cx - radius)..(cx + radius)) {
            for (by in (cy - 1)..(cy + 1)) {
                for (bz in (cz - radius)..(cz + radius)) {
                    val bpos = BlockPos(bx, by, bz)
                    freezeWaterAt(serverLevel, bpos)
                }
            }
        }
    }

    private fun extinguishFiresInRadius(serverLevel: ServerLevel, center: Vec3, radius: Int) {
        val cx = center.x.toInt()
        val cy = center.y.toInt()
        val cz = center.z.toInt()

        for (bx in (cx - radius)..(cx + radius)) {
            for (by in (cy - 2)..(cy + 2)) {
                for (bz in (cz - radius)..(cz + radius)) {
                    val bpos = BlockPos(bx, by, bz)
                    val state = serverLevel.getBlockState(bpos)
                    if (state.`is`(Blocks.FIRE) || state.`is`(Blocks.SOUL_FIRE)) {
                        serverLevel.destroyBlock(bpos, false)
                    } else if (state.block is CampfireBlock && state.getValue(CampfireBlock.LIT)) {
                        CampfireBlock.dowse(getThrower(), serverLevel, bpos, state)
                    }
                }
            }
        }
    }

    private fun spawnClientVisuals() {
        // Particules réservées
    }

    fun getThrower(): Player? {
        val uuid = throwerUuid ?: return null
        return level().getPlayerByUUID(uuid)
    }

    var boomerangState: BoomerangState
        get() = BoomerangState.fromId(entityData.get(BOOMERANG_STATE))
        set(value) = entityData.set(BOOMERANG_STATE, value.id)

    var flightTicks: Int
        get() = entityData.get(FLIGHT_TICKS)
        set(value) = entityData.set(FLIGHT_TICKS, value)

    var hoverTicks: Int
        get() = entityData.get(HOVER_TICKS)
        set(value) = entityData.set(HOVER_TICKS, value)

    var carriedItemCount: Int
        get() = entityData.get(CARRIED_ITEM_COUNT)
        set(value) = entityData.set(CARRIED_ITEM_COUNT, value)

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        // Procédural
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = geoCache

    override fun hurtServer(serverLevel: ServerLevel, damageSource: DamageSource, amount: Float): Boolean = false

    override fun isPushable(): Boolean = false

    override fun isPickable(): Boolean = false
}
