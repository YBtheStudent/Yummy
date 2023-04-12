package com.lalaalal.yummy.entity;

import com.lalaalal.yummy.block.PollutedBlock;
import com.lalaalal.yummy.block.YummyBlockRegister;
import com.lalaalal.yummy.block.entity.PollutedBlockEntity;
import com.lalaalal.yummy.effect.YummyEffectRegister;
import com.lalaalal.yummy.entity.goal.SkillUseGoal;
import com.lalaalal.yummy.entity.skill.*;
import com.lalaalal.yummy.misc.PhaseManager;
import com.lalaalal.yummy.networking.YummyMessages;
import com.lalaalal.yummy.networking.packet.ToggleHerobrineMusicPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;

public class Herobrine extends PathfinderMob implements SkillUsable {
    private static final float[] PHASE_HEALTHS = {600, 60, 6};
    private static final BossEvent.BossBarColor[] PHASE_COLORS = {BossEvent.BossBarColor.BLUE, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarColor.PINK};

    private static final EntityDataAccessor<Integer> DATA_SKILL_USE_ID = SynchedEntityData.defineId(Herobrine.class, EntityDataSerializers.INT);
    private final ArrayList<BlockPos> blockPosList = new ArrayList<>();
    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS))
            .setDarkenScreen(true)
            .setPlayBossMusic(true);
    private final PhaseManager phaseManager = new PhaseManager(PHASE_HEALTHS, PHASE_COLORS, this);
    private int invulnerableTick = 0;
    private static final int INVULNERABLE_DURATION = 20 * 5;
    private BlockPos initialPos;
    private boolean usingSkill = false;

    public static boolean canSummonHerobrine(Level level, BlockPos headPos) {
        Block soulSandBlock = level.getBlockState(headPos).getBlock();
        Block netherBlock = level.getBlockState(headPos.below(1)).getBlock();
        Block goldBlock1 = level.getBlockState(headPos.below(2)).getBlock();
        Block goldBlock2 = level.getBlockState(headPos.below(3)).getBlock();

        return soulSandBlock == Blocks.SOUL_SAND
                && netherBlock == Blocks.CHISELED_NETHER_BRICKS
                && goldBlock1 == Blocks.GOLD_BLOCK
                && goldBlock2 == Blocks.GOLD_BLOCK;
    }

    public static void polluteHerobrineAlter(Level level, BlockPos headPos) {
        level.setBlock(headPos.above(), YummyBlockRegister.PURIFIED_SOUL_FIRE_BLOCK.get().defaultBlockState(), 10);
        level.setBlock(headPos, YummyBlockRegister.DISPLAYING_POLLUTED_BLOCK.get()
                .defaultBlockState()
                .setValue(PollutedBlock.POWERED, true), 10);
        level.setBlock(headPos.below(), YummyBlockRegister.DISPLAYING_POLLUTED_BLOCK.get().defaultBlockState(), 10);
        level.setBlock(headPos.below(1), YummyBlockRegister.DISPLAYING_POLLUTED_BLOCK.get().defaultBlockState(), 10);
        level.setBlock(headPos.below(2), YummyBlockRegister.DISPLAYING_POLLUTED_BLOCK.get()
                .defaultBlockState()
                .setValue(PollutedBlock.CORRUPTED, true), 10);
        level.setBlock(headPos.below(3), YummyBlockRegister.DISPLAYING_POLLUTED_BLOCK.get()
                .defaultBlockState()
                .setValue(PollutedBlock.CORRUPTED, true), 10);
    }

    public static void destroySpawnStructure(Level level, BlockPos headPos) {
        level.destroyBlock(headPos, false);
        level.destroyBlock(headPos.below(1), false);
        level.destroyBlock(headPos.below(2), false);
        level.destroyBlock(headPos.below(3), false);
    }

    public static AttributeSupplier.Builder getHerobrineAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.MAX_HEALTH, 666)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.ATTACK_DAMAGE, 16)
                .add(Attributes.ATTACK_KNOCKBACK, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    public Herobrine(EntityType<? extends Herobrine> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.xpReward = 666;
        this.entityData.define(DATA_SKILL_USE_ID, 0);

        this.phaseManager.addPhaseChangeListener(this::changePhase);
        this.phaseManager.addPhaseChangeListener(this::enterPhase2, 2);
        this.phaseManager.addPhaseChangeListener(this::enterPhase3, 3);
        setPersistenceRequired();
    }

    @Override
    public boolean isUsingSkill() {
        return usingSkill;
    }

    @Override
    public void setUsingSkill(boolean usingSkill) {
        this.usingSkill = usingSkill;
    }

    public void setInitialPos(BlockPos blockPos) {
        initialPos = blockPos;
    }

    public int getPhase() {
        return phaseManager.getCurrentPhase();
    }

    public void setArmPose(ArmPose armPose) {
        entityData.set(DATA_SKILL_USE_ID, armPose.getId());
    }

    public ArmPose getArmPose() {
        int armPoseID = entityData.get(DATA_SKILL_USE_ID);
        return ArmPose.byId(armPoseID);
    }

    private int[] blockPosToIntArray(BlockPos blockPos) {
        return new int[]{blockPos.getX(), blockPos.getY(), blockPos.getZ()};
    }

    private BlockPos blockPosFromIntArray(int[] array) {
        return new BlockPos(array[0], array[1], array[2]);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("numPollutedBlocks", blockPosList.size());
        for (int i = 0; i < blockPosList.size(); i++)
            tag.putIntArray("blockPosList" + i, blockPosToIntArray(blockPosList.get(i)));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        Level level = getLevel();
        int numPollutedBlocks = tag.getInt("numPollutedBlocks");
        for (int i = 0; i < numPollutedBlocks; i++) {
            int[] array = tag.getIntArray("blockPosList" + i);
            BlockPos blockPos = blockPosFromIntArray(array);
            blockPosList.add(blockPos);
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof PollutedBlockEntity pollutedBlockEntity)
                pollutedBlockEntity.setHerobrine(this);
        }
    }

    public boolean canSummonPollutedBlock() {
        int maxPollutedBlock = getPhase() != phaseManager.getMaxPhase() ? 6 : 10;

        return blockPosList.size() < maxPollutedBlock;
    }

    public void addPollutedBlock(PollutedBlockEntity pollutedBlockEntity) {
        blockPosList.add(pollutedBlockEntity.getBlockPos());
        pollutedBlockEntity.setHerobrine(this);
    }

    public void removePollutedBlock(PollutedBlockEntity pollutedBlockEntity) {
        blockPosList.remove(pollutedBlockEntity.getBlockPos());
    }

    @Override
    protected void customServerAiStep() {
        if (invulnerableTick < INVULNERABLE_DURATION) {
            invulnerableTick += 1;
        } else if (invulnerableTick == INVULNERABLE_DURATION) {
            setInvulnerable(false);
        }

        phaseManager.updateBossProgressBar(bossEvent);
    }

    private void changePhase(int phase) {
        invulnerableTick = 0;
        LevelChunk levelChunk = level.getChunkAt(getOnPos());
        YummyMessages.sendToPlayer(new ToggleHerobrineMusicPacket(true, phase), levelChunk);
        setInvulnerable(true);
        setHealth(phaseManager.getActualCurrentPhaseMaxHealth());
    }

    private void enterPhase2() {
        AttributeInstance attributeInstance = getAttribute(Attributes.ARMOR);
        if (attributeInstance != null)
            attributeInstance.setBaseValue(20);
    }

    private void enterPhase3() {
        if (initialPos != null) {
            destroySpawnStructure(level, initialPos);
            moveTo(initialPos, 0, 0);
        }
        level.explode(this, getX(), getY(), getZ(), 6, false, Explosion.BlockInteraction.NONE);
        goalSelector.removeAllGoals();
        targetSelector.removeAllGoals();
        getNavigation().stop();

        setArmPose(ArmPose.RAISE_BOTH);
        setUsingSkill(false);
        this.goalSelector.addGoal(1, new SkillUseGoal(this, new KnockbackAndMarkSkill(this)));
        this.goalSelector.addGoal(2, new SkillUseGoal(this, new AddDigSlownessSkill(this)));
        this.goalSelector.addGoal(3, new SkillUseGoal(this, new SummonPollutedBlockSkill(this, 10)));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new SkillUseGoal(this, new ShootFireballSkill(this)));
        this.goalSelector.addGoal(2, new SkillUseGoal(this, new TeleportAndShootMeteorSkill(this)));
        this.goalSelector.addGoal(3, new SkillUseGoal(this, new ExplosionSkill(this)));
        this.goalSelector.addGoal(2, new SkillUseGoal(this, new SummonPollutedBlockSkill(this)));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, false, false));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.isBypassInvul())
            return super.hurt(source, amount);
        if (random.nextInt(0, 10) == 5) {
            if (source.getEntity() instanceof LivingEntity livingEntity) {
                MobEffectInstance mobEffectInstance = new MobEffectInstance(YummyEffectRegister.STUN.get(), 60, 0);
                livingEntity.addEffect(mobEffectInstance);
            }
        }
        if (invulnerableTick < INVULNERABLE_DURATION)
            return true;

        int maxPhase = phaseManager.getMaxPhase();
        if (getPhase() != maxPhase && getHealth() - amount < 0) {
            super.hurt(source, 0.1f);
            setHealth(phaseManager.getPhaseMaxHealth(maxPhase));

            return true;
        }
        if (getPhase() == maxPhase) {
            if (source.isMagic())
                return super.hurt(source.bypassArmor(), 1f);
            return true;
        }

        return super.hurt(source, amount);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer serverPlayer) {
        super.startSeenByPlayer(serverPlayer);
        bossEvent.addPlayer(serverPlayer);
        phaseManager.updateBossProgressBar(bossEvent);
        YummyMessages.sendToPlayer(new ToggleHerobrineMusicPacket(true, getPhase()), serverPlayer);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer serverPlayer) {
        super.stopSeenByPlayer(serverPlayer);
        bossEvent.removePlayer(serverPlayer);

        YummyMessages.sendToPlayer(new ToggleHerobrineMusicPacket(false, 1), serverPlayer);
        YummyMessages.sendToPlayer(new ToggleHerobrineMusicPacket(false, 2), serverPlayer);
        YummyMessages.sendToPlayer(new ToggleHerobrineMusicPacket(false, 3), serverPlayer);
    }

    public enum ArmPose {
        NORMAL(0),
        RAISE_RIGHT(1),
        RAISE_LEFT(2),
        RAISE_BOTH(3);

        final int id;

        public static ArmPose byId(int id) {
            return switch (id) {
                case 1 -> RAISE_RIGHT;
                case 2 -> RAISE_LEFT;
                case 3 -> RAISE_BOTH;
                default -> NORMAL;
            };
        }

        ArmPose(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
