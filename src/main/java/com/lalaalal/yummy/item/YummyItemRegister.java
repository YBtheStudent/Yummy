package com.lalaalal.yummy.item;

import com.lalaalal.yummy.YummyMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class YummyItemRegister {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YummyMod.MOD_ID);
    private static final DeferredRegister<Item> VANILLA_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "minecraft");

    public static final RegistryObject<Item> NEMETHYST_ITEM = ITEMS.register("nemethyst",
            () -> new Item(new Item.Properties().tab(YummyMod.TAB)));
    public static final RegistryObject<Item> BREATH_STEEL_INGOT = ITEMS.register("breath_steel_ingot",
            () -> new Item(new Item.Properties().tab(YummyMod.TAB)));
    public static final RegistryObject<Item> ECHO_SILVER_SHARD_ITEM = ITEMS.register("echo_silver_shard",
            () -> new Item(new Item.Properties().tab(YummyMod.TAB)));
    public static final RegistryObject<Item> SPEAR_OF_LONGINUS_ITEM = ITEMS.register("spear_of_longinus", SpearOfLonginusItem::new);

    public static final RegistryObject<Item> BONE_MEAL = VANILLA_ITEMS.register("bone_meal", YummyBoneMealItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        VANILLA_ITEMS.register(eventBus);
    }
}
