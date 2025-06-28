package com.cjcrafter.vivecraft.compatibility;

import com.cjcrafter.foliascheduler.util.FieldAccessor;
import com.cjcrafter.foliascheduler.util.MethodInvoker;
import com.cjcrafter.foliascheduler.util.ReflectionUtil;
import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent;
import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftCreeper;
import org.bukkit.craftbukkit.entity.CraftEnderman;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.cjcrafter.vivecraft.VSE;
import com.cjcrafter.vivecraft.VivePlayer;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

public class Vivecraft_v1_21_R2 implements VivecraftCompatibility {

    private static Class<?> classEndermanFreezeWhenLookedAt;
    private static Class<?> classEndermanLookForPlayerGoal;
    private static FieldAccessor poseAccessor;
    private static FieldAccessor itemsByIdAccessor;
    private static FieldAccessor eyeHeightAccessor;
    private static MethodInvoker resetFallDistanceMethod;


    public Vivecraft_v1_21_R2() {
        classEndermanFreezeWhenLookedAt = ReflectionUtil.getMinecraftClass("world.entity.monster", "EntityEnderman$a"); // https://mappings.dev/1.21.3/net/minecraft/world/entity/monster/EnderMan$EndermanFreezeWhenLookedAt.html
        classEndermanLookForPlayerGoal = ReflectionUtil.getMinecraftClass("world.entity.monster", "EntityEnderman$PathfinderGoalPlayerWhoLookedAtTarget");
        poseAccessor = ReflectionUtil.getField(Entity.class, EntityDataAccessor.class, 6, ReflectionUtil.IS_STATIC);
        itemsByIdAccessor = ReflectionUtil.getField(SynchedEntityData.class, SynchedEntityData.DataItem[].class);
        eyeHeightAccessor = ReflectionUtil.getField(Entity.class, "bc");  // https://mappings.dev/1.21.3/net/minecraft/world/entity/Entity.html
        resetFallDistanceMethod = ReflectionUtil.getMethod(Entity.class, "k");
    }

    @Override
    public void injectCreeper(Creeper creeper, double radius) {
        net.minecraft.world.entity.monster.Creeper e = ((CraftCreeper) creeper).getHandle();
        Set<WrappedGoal> goals = e.goalSelector.getAvailableGoals();
        goals.removeIf(SwellGoal.class::isInstance);
        e.goalSelector.addGoal(2, new CustomGoalSwell(e, radius));
    }

    @Override
    public void injectEnderman(Enderman enderman) {
        EnderMan e = ((CraftEnderman) enderman).getHandle();
        Collection<WrappedGoal> targets = e.targetSelector.getAvailableGoals();
        targets.removeIf(classEndermanLookForPlayerGoal::isInstance);

        e.targetSelector.addGoal(1, new CustomPathFinderGoalPlayerWhoLookedAtTarget(e, e::isAngryAt));

        Collection<WrappedGoal> goals = e.goalSelector.getAvailableGoals();
        goals.removeIf(classEndermanFreezeWhenLookedAt::isInstance);
        e.goalSelector.addGoal(1, new CustomGoalStare(e));
    }

    @Override
    public void injectPlayer(Player bukkit) {
        ServerPlayer player = ((CraftPlayer) bukkit).getHandle();
        player.connection.connection.channel.pipeline().addBefore("packet_handler", "vr_aim_fix", new AimFixHandler(player.connection.connection));
    }

    @Override
    public void injectPoseOverrider(Player bukkit) {
        ServerPlayer player = ((CraftPlayer) bukkit).getHandle();
        EntityDataAccessor<Pose> poseObj = (EntityDataAccessor<Pose>) poseAccessor.get(player);
        SynchedEntityData.DataItem<?>[] items = (SynchedEntityData.DataItem<?>[]) itemsByIdAccessor.get(player.getEntityData());
        items[poseObj.id()] = new InjectedDataWatcherItem(poseObj, Pose.STANDING, bukkit);
    }

    @Override
    public void resetFall(Player bukkit) {
        net.minecraft.world.entity.player.Player player = ((CraftPlayer) bukkit).getHandle();
        resetFallDistanceMethod.invoke(player);
    }

    @Override
    public org.bukkit.inventory.ItemStack setLocalizedName(org.bukkit.inventory.ItemStack item, String key, String fallback) {
        var nmsStack = CraftItemStack.asNMSCopy(item);
        nmsStack.set(DataComponents.CUSTOM_NAME, Component.translatableWithFallback(key, fallback));
        return CraftItemStack.asBukkitCopy(nmsStack);
    }

    @Override
    public void setSwimming(Player player) {
        ((CraftPlayer) player).getHandle().setPose(Pose.SWIMMING);
    }

    @Override
    public void absMoveTo(Player bukkit, double x, double y, double z) {
        ServerPlayer player = ((CraftPlayer) bukkit).getHandle();
        player.absMoveTo(x, y, z);
    }

    public static class AimFixHandler extends ChannelInboundHandlerAdapter {
        private final Connection netManager;

        public AimFixHandler(Connection netManager) {
            this.netManager = netManager;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            net.minecraft.world.entity.player.Player player = ((ServerGamePacketListenerImpl) netManager.getPacketListener()).player;
            boolean isCapturedPacket = msg instanceof ServerboundUseItemPacket || msg instanceof ServerboundUseItemOnPacket || msg instanceof ServerboundPlayerActionPacket;
            UUID uuid = player.getGameProfile().getId();
            if (!VSE.vivePlayers.containsKey(uuid) || !VSE.vivePlayers.get(uuid).isVR() || !isCapturedPacket || player.getServer() == null) {
                // we don't need to handle this packet, just defer to the next handler in the pipeline
                ctx.fireChannelRead(msg);
                return;
            }

            player.getServer().submit(() -> {
                // Save all the current orientation data
                Vec3 oldPos = player.position();
                Vec3 oldPrevPos = new Vec3(player.xo, player.yo, player.zo);
                float oldPitch = player.getXRot();
                float oldYaw = player.getYRot();
                float oldYawHead = player.yHeadRot; // field_70759_as
                float oldPrevPitch = player.xRotO;
                float oldPrevYaw = player.yRotO;
                float oldPrevYawHead = player.yHeadRotO; // field_70758_at
                float oldEyeHeight = player.getEyeHeight();

                VivePlayer data = null;
                if (VSE.vivePlayers.containsKey(uuid) && VSE.vivePlayers.get(uuid).isVR()) { // Check again in case of race condition
                    data = VSE.vivePlayers.get(uuid);
                    Location pos = data.getControllerPos(0);
                    Vector aim = data.getControllerDir(0);

                    // Inject our custom orientation data
                    player.setPosRaw(pos.getX(), pos.getY(), pos.getZ());
                    player.xo = pos.getX();
                    player.yo = pos.getY();
                    player.zo = pos.getZ();
                    player.setXRot((float) Math.toDegrees(Math.asin(-aim.getY())));
                    player.setYRot((float) Math.toDegrees(Math.atan2(-aim.getX(), aim.getZ())));
                    player.xRotO = player.getXRot();
                    player.yRotO = player.yHeadRotO = player.yHeadRot = player.getYRot();
                    eyeHeightAccessor.set(player, 0f);

                    // Set up offset to fix relative positions
                    // P.S. Spigot mappings are stupid
                    Vec3 nms = oldPos.add(-pos.getX(), -pos.getY(), -pos.getZ());
                    data.offset = new Vector(nms.x, nms.y, nms.z);
                }

                // Call the packet handler directly
                // This is several implementation details that we have to replicate
                try {
                    if (netManager.isConnected()) {
                        try {
                            ((Packet) msg).handle(this.netManager.getPacketListener());
                        } catch (RunningOnDifferentThreadException runningondifferentthreadexception) {
                        }
                    }
                } finally {
                    // Vanilla uses SimpleInboundChannelHandler, which automatically releases
                    // by default, so we're expected to release the packet once we're done.
                    ReferenceCountUtil.release(msg);
                }

                // Restore the original orientation data
                player.setPosRaw(oldPos.x, oldPos.y, oldPos.z);
                player.xo = oldPrevPos.x;
                player.yo = oldPrevPos.y;
                player.zo = oldPrevPos.z;
                player.setXRot(oldPitch);
                player.setYRot(oldYaw);
                player.yHeadRot = oldYawHead;
                player.xRotO = oldPrevPitch;
                player.yRotO = oldPrevYaw;
                player.yHeadRotO = oldPrevYawHead;
                eyeHeightAccessor.set(player, 0f);

                // Reset offset
                if (data != null)
                    data.offset = new Vector(0, 0, 0);
            });
        }
    }


    public static class InjectedDataWatcherItem extends SynchedEntityData.DataItem<Pose> {
        protected final Player player;

        public InjectedDataWatcherItem(EntityDataAccessor<Pose> datawatcherobject, Pose t0, Player player) {
            super(datawatcherobject, t0);
            this.player = player;
        }

        @Override
        public void setValue(Pose pose) {
            VivePlayer vp = VSE.vivePlayers.get(player.getUniqueId());
            if (vp != null && vp.isVR() && vp.crawling)
                super.setValue(Pose.SWIMMING);
            else
                super.setValue(pose);
        }
    }


    public static class CustomGoalSwell extends SwellGoal {

        private final net.minecraft.world.entity.monster.Creeper creeper;
        public double radiusSqr;

        public CustomGoalSwell(net.minecraft.world.entity.monster.Creeper var0, double radius) {
            super(var0);

            this.creeper = var0;
            this.radiusSqr = radius * radius;
        }

        @Override
        public boolean canUse() {
            LivingEntity livingEntity = this.creeper.getTarget();

            // usually you want VR players to have a smaller radius since it is
            // harder to play in VR.
            double distance = 9.0;
            if (livingEntity != null && livingEntity.getBukkitEntity() instanceof Player player && VSE.isVive(player)) {
                distance = radiusSqr;
            }

            return this.creeper.getSwellDir() > 0 || livingEntity != null && this.creeper.distanceToSqr(livingEntity) < distance;
        }
    }

    public static class CustomPathFinderGoalPlayerWhoLookedAtTarget extends NearestAttackableTargetGoal<net.minecraft.world.entity.player.Player> {

        private final EnderMan enderman;
        private final TargetingConditions startAggroTargetConditions;
        private final TargetingConditions continueAggroTargetConditions = TargetingConditions.forCombat().ignoreLineOfSight();
        private final TargetingConditions.Selector isAngerInducing;
        private net.minecraft.world.entity.player.Player pendingTarget;
        private int aggroTime;
        private int teleportTime;

        public CustomPathFinderGoalPlayerWhoLookedAtTarget(EnderMan entityenderman, TargetingConditions.Selector targetPredicate) {
            super(entityenderman, net.minecraft.world.entity.player.Player.class, 10, false, false, targetPredicate);
            this.enderman = entityenderman;
            this.isAngerInducing = (entityliving, worldserver) -> (isBeingStaredBy(enderman, (net.minecraft.world.entity.player.Player) entityliving) || enderman.isAngryAt(entityliving, worldserver)) && !enderman.hasIndirectPassenger(entityliving);
            this.startAggroTargetConditions = TargetingConditions.forCombat().range(this.getFollowDistance()).selector(this.isAngerInducing);
        }

        public boolean canUse() {
            this.pendingTarget = getServerLevel(this.enderman).getNearestPlayer(this.startAggroTargetConditions.range(this.getFollowDistance()), this.enderman);
            return this.pendingTarget != null;
        }

        public void start() {
            this.aggroTime = this.adjustedTickDelay(5);
            this.teleportTime = 0;
            this.enderman.setBeingStaredAt();
        }

        public void stop() {
            this.pendingTarget = null;
            super.stop();
        }

        public boolean canContinueToUse() {
            if (this.pendingTarget != null) {
                if (!this.isAngerInducing.test(this.pendingTarget, getServerLevel(this.enderman))) {
                    return false;
                } else {
                    this.enderman.lookAt(this.pendingTarget, 10.0F, 10.0F);
                    return true;
                }
            } else {
                if (super.target != null) {
                    if (this.enderman.hasIndirectPassenger(super.target)) {
                        return false;
                    }

                    if (this.continueAggroTargetConditions.test(getServerLevel(this.enderman), this.enderman, super.target)) {
                        return true;
                    }
                }

                return super.canContinueToUse();
            }
        }

        public void tick() {
            if (this.enderman.getTarget() == null) {
                super.setTarget((LivingEntity)null);
            }

            if (this.pendingTarget != null) {
                if (--this.aggroTime <= 0) {
                    super.target = this.pendingTarget;
                    this.pendingTarget = null;
                    super.start();
                }
            } else {
                if (super.target != null && !this.enderman.isPassenger()) {
                    if (isBeingStaredBy(enderman, (net.minecraft.world.entity.player.Player)super.target)) {
                        if (super.target.distanceToSqr(this.enderman) < (double)16.0F && new EndermanEscapeEvent((CraftEnderman)enderman.getBukkitEntity(), EndermanEscapeEvent.Reason.STARE).callEvent()) {
                            this.enderman.teleport();
                        }

                        this.teleportTime = 0;
                    } else if (super.target.distanceToSqr(this.enderman) > (double)256.0F && this.teleportTime++ >= this.adjustedTickDelay(30) && this.enderman.teleportTowards(super.target)) {
                        this.teleportTime = 0;
                    }
                }

                super.tick();
            }

        }

        //Vivecraft copy from EnderMan
        public static boolean isBeingStaredBy(EnderMan enderman, net.minecraft.world.entity.player.Player player) {
            boolean shouldAttack = isBeingStaredBy0(enderman, player);
            EndermanAttackPlayerEvent event = new EndermanAttackPlayerEvent((Enderman) enderman.getBukkitEntity(), (org.bukkit.entity.Player) player.getBukkitEntity());
            event.setCancelled(!shouldAttack);
            return event.callEvent();
        }

        //Vivecraft copy from EnderMan
        private static boolean isBeingStaredBy0(EnderMan enderman, net.minecraft.world.entity.player.Player player) {
            return isLookingAtMe(enderman, player, 0.025, true, false, LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM, enderman::getEyeY);
        }

        // Vivecraft copy and modify from LivingEntity
        public static boolean isLookingAtMe(Entity enderman, LivingEntity entity, double d0, boolean flag, boolean visualShape, Predicate<LivingEntity> predicate, DoubleSupplier... entityYChecks) {
            if (!predicate.test(entity))
                return false;

            Vec3 viewVector = entity.getViewVector(1.0F).normalize();
            Vec3 origin = new Vec3(entity.getX(), entity.getY(), entity.getZ());

            boolean isVr = false;
            if (entity instanceof net.minecraft.world.entity.player.Player nmsPlayer) {
                Player player = (Player) nmsPlayer.getBukkitEntity();
                isVr = VSE.isVive(player);
                if (isVr) {
                    VivePlayer vive = VSE.vivePlayers.get(player.getUniqueId());
                    Vector hmdDir = vive.getHMDDir();
                    viewVector = new Vec3(hmdDir.getX(), hmdDir.getY(), hmdDir.getZ());
                    Location hmdLoc = vive.getHMDPos();
                    origin = new Vec3(hmdLoc.getX(), hmdLoc.getY(), hmdLoc.getZ());
                }
            }

            for (DoubleSupplier doublesupplier : entityYChecks) {
                Vec3 vec3d1 = new Vec3(enderman.getX() - origin.x(), doublesupplier.getAsDouble() - origin.y(), enderman.getZ() - origin.z());
                double d1 = vec3d1.length();
                vec3d1 = vec3d1.normalize();
                double d2 = viewVector.dot(vec3d1);
                if (d2 > (double) 1.0F - d0 / (flag ? d1 : (double) 1.0F)) {
                    if (isVr) {
                        return hasLineOfSight(entity, origin, enderman, visualShape ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, doublesupplier);
                    } else {
                        return entity.hasLineOfSight(enderman, visualShape ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, doublesupplier);
                    }
                }
            }

            return false;
        }

        //Vivecraft copy and modify from LivingEntity
        public static boolean hasLineOfSight(Entity source, Vec3 origin, Entity enderman, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, DoubleSupplier entityY) {
            if (enderman.level() != source.level()) {
                return false;
            } else {
                Vec3 target = new Vec3(enderman.getX(), entityY.getAsDouble(), enderman.getZ());
                return target.distanceToSqr(origin) > (double)16384.0F ? false : source.level().clip(new ClipContext(origin, target, shapeType, fluidHandling, source)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
            }
        }
    }

    /**
     * Based off of the EndermanFreezeWhenLookedAt, but it is private so we
     * cannot extend it.
     */
    public static class CustomGoalStare extends Goal {
        private final EnderMan enderman;
        @Nullable
        private LivingEntity target;

        public CustomGoalStare(EnderMan enderman) {
            this.enderman = enderman;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        public boolean canUse() {
            this.target = this.enderman.getTarget();
            if (!(this.target instanceof net.minecraft.world.entity.player.Player)) {
                return false;
            } else {
                double d0 = this.target.distanceToSqr(this.enderman);
                return d0 > (double)256.0F ? false : CustomPathFinderGoalPlayerWhoLookedAtTarget.isBeingStaredBy(enderman, (net.minecraft.world.entity.player.Player)this.target);
            }
        }

        public void start() {
            this.enderman.getNavigation().stop();
        }

        public void tick() {
            this.enderman.getLookControl().setLookAt(this.target.getX(), this.target.getEyeY(), this.target.getZ());
        }
    }
}
