package com.kodyris.bluesteel;

import com.kodyris.bluesteel.block.KillBlock;
import com.kodyris.bluesteel.block.RedBarrierBlock;
import com.kodyris.bluesteel.block.TriggerBlock;
import com.kodyris.bluesteel.block.TriggerBlockEntity;
import com.kodyris.bluesteel.network.ModNetwork;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TlsAdditionalBlocksMod.MODID)
public class TlsAdditionalBlocksMod {
    // The mod id stays "bluesteel" so blocks already placed in existing worlds keep working.
    public static final String MODID = "bluesteel";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    // IronBarsBlock is the vanilla "pane" logic: thin post + arms that connect to
    // neighbouring panes, walls and solid faces. Same hardness/sound as iron bars.
    public static final RegistryObject<Block> BLUE_STEEL_PANE = BLOCKS.register("blue_steel_pane",
            () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> BLUE_STEEL_PANE_ITEM = ITEMS.register("blue_steel_pane",
            () -> new BlockItem(BLUE_STEEL_PANE.get(), new Item.Properties()));

    // Same properties as vanilla's barrier. Invisible; the red outline is drawn client-side.
    public static final RegistryObject<Block> BARRIER = BLOCKS.register("barrier",
            () -> new RedBarrierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3600000.8F)
                    .noLootTable()
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .noParticlesOnBreak()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Item> BARRIER_ITEM = ITEMS.register("barrier",
            () -> new BlockItem(BARRIER.get(), new Item.Properties()));

    // Barrier-style block, but non-solid, and it kills any living entity that enters it
    // (creative players and operators included).
    public static final RegistryObject<Block> KILL_BLOCK = BLOCKS.register("kill_block",
            () -> new KillBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3600000.8F)
                    .noLootTable()
                    .noOcclusion()
                    .noCollission()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .noParticlesOnBreak()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Item> KILL_BLOCK_ITEM = ITEMS.register("kill_block",
            () -> new BlockItem(KILL_BLOCK.get(), new Item.Properties()));

    // Barrier-style, non-solid block that runs a command when an entity enters it.
    // Untargetable unless the config stick is held; right-click with the stick to configure.
    public static final RegistryObject<Block> TRIGGER_BLOCK = BLOCKS.register("trigger_block",
            () -> new TriggerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(-1.0F, 3600000.8F)
                    .noLootTable()
                    .noOcclusion()
                    .noCollission()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .noParticlesOnBreak()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Item> TRIGGER_BLOCK_ITEM = ITEMS.register("trigger_block",
            () -> new BlockItem(TRIGGER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<TriggerBlockEntity>> TRIGGER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("trigger_block",
                    () -> BlockEntityType.Builder.of(TriggerBlockEntity::new, TRIGGER_BLOCK.get()).build(null));

    public static final RegistryObject<Item> CONFIG_STICK = ITEMS.register("config_stick",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public TlsAdditionalBlocksMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        ModNetwork.register();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(BLUE_STEEL_PANE_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS && event.hasPermissions()) {
            event.accept(BARRIER_ITEM);
            event.accept(KILL_BLOCK_ITEM);
            event.accept(TRIGGER_BLOCK_ITEM);
            event.accept(CONFIG_STICK);
        }
    }
}
