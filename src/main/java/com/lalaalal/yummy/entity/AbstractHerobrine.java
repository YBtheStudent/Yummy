package com.lalaalal.yummy.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import software.bernie.geckolib3.core.IAnimatable;

public abstract class AbstractHerobrine extends CameraShakingEntity implements IAnimatable, Enemy {
    public final boolean isShadow;

    protected AbstractHerobrine(EntityType<? extends AbstractHerobrine> entityType, Level level, boolean isShadow) {
        super(entityType, level);
        this.isShadow = isShadow;
    }
}
