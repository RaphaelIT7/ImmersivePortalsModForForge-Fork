package qouteall.imm_ptl.peripheral;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import qouteall.imm_ptl.peripheral.alternate_dimension.*;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;
import qouteall.imm_ptl.peripheral.mixin.common.end_portal.IEEndDragonFight;
import qouteall.imm_ptl.peripheral.platform_specific.PeripheralModEntry;
import qouteall.imm_ptl.peripheral.platform_specific.PeripheralModEntryClient;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;
import qouteall.q_misc_util.LifecycleHack;
import qouteall.q_misc_util.MiscHelper;

import javax.annotation.Nullable;
import java.util.List;

public class PeripheralModMain {
    public static final String MODID = "immersive_portals";

    private static void registerBlockItems() {
        //PeripheralModMain.registerCommandStickTypes();

        CommandStickItem.init();
    }

    @SubscribeEvent
    public void buildContents(BuildCreativeModeTabContentsEvent event) {
        // Add to creative tab
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PORTAL_HELPER_ITEM.get().getDefaultInstance());
            event.accept(PORTAL_HELPER_ITEM.get().getDefaultInstance());
        }
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PeripheralModEntryClient.onInitializeClient();
    }

    public static class PortalHelperItem extends BlockItem {

        public PortalHelperItem(Block block, Properties settings) {
            super(block, settings);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (context.getLevel().isClientSide()) {
                if (context.getPlayer() != null) {
                    IPOuterClientMisc.onClientPlacePortalHelper();
                }
            }

            return super.useOn(context);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
            super.appendHoverText(stack, world, tooltip, context);

            tooltip.add(Component.translatable("imm_ptl.portal_helper_tooltip"));
        }
    }

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Block> PORTAL_HELPER_BLOCK = BLOCKS.register("portal_helper", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).noOcclusion().isRedstoneConductor((a, b, c) -> false)));
    public static final RegistryObject<Item> PORTAL_HELPER_ITEM = ITEMS.register("portal_helper", () -> new PortalHelperItem(PORTAL_HELPER_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> COMMAND_STICK_ITEM = ITEMS.register("command_stick", () -> new CommandStickItem(new Item.Properties()));


    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        IPOuterClientMisc.initClient();
    }

    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();

        DimStackManagement.init();
        
        AlternateDimensions.init();

        LifecycleHack.markNamespaceStable("immersive_portals");
        LifecycleHack.markNamespaceStable("imm_ptl");

        FMLJavaModLoadingContext.get().getModEventBus().register(PeripheralModEntry.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CommandStickItem::buildContents);
        registerBlockItems(); //TODO Move this to a real DeferredRegistry @Nick1st
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        PeripheralRegistries.CHUNK_GENERATOR.register(FMLJavaModLoadingContext.get().getModEventBus());
        PeripheralRegistries.BIOME_SOURCE.register(FMLJavaModLoadingContext.get().getModEventBus());
        CommandStickItem.CommandStickData.register(FMLJavaModLoadingContext.get().getModEventBus());

        CommandStickItem.init();


        //CommandStickItem.registerCommandStickTypes();

//        Registry.register( //Fixme removal
//            BuiltInRegistries.CHUNK_GENERATOR,
//            new ResourceLocation("immersive_portals:error_terrain_generator"),
//            ErrorTerrainGenerator.codec
//        );
//        Registry.register(
//            BuiltInRegistries.CHUNK_GENERATOR,
//            new ResourceLocation("immersive_portals:normal_skyland_generator"),
//            NormalSkylandGenerator.codec
//        );
    
//        BuiltInRegistries.register(
//            Registry.BIOME_SOURCE,
//            new ResourceLocation("immersive_portals:chaos_biome_source"),
//            ChaosBiomeSource.CODEC
//        );

        /*EndPortalEntity.updateDragonFightStatusFunc = () -> {
            ServerLevel world = MiscHelper.getServer().getLevel(Level.END);
            if (world == null) {
                return;
            }
            EndDragonFight dragonFight = world.dragonFight();
            if (dragonFight == null) {
                return;
            }
            if (((IEEndDragonFight) dragonFight).ip_getNeedsStateScanning()) {
                ((IEEndDragonFight) dragonFight).ip_scanState();
            }
        };*/
    }
    
   /* public static void registerCommandStickTypes() {
//        registerPortalSubCommandStick("delete_portal");
//        registerPortalSubCommandStick("remove_connected_portals");
//        registerPortalSubCommandStick("eradicate_portal_cluster");
//        registerPortalSubCommandStick("complete_bi_way_bi_faced_portal");
//        registerPortalSubCommandStick("complete_bi_way_portal");
//        registerPortalSubCommandStick("move_portal_front", "move_portal 0.5");
//        registerPortalSubCommandStick("move_portal_back", "move_portal -0.5");
//        registerPortalSubCommandStick(
//            "move_portal_destination_front", "move_portal_destination 0.5"
//        );
//        registerPortalSubCommandStick(
//            "move_portal_destination_back", "move_portal_destination -0.5"
//        );
//        registerPortalSubCommandStick(
//            "rotate_x", "rotate_portal_rotation_along x 15"
//        );
//        registerPortalSubCommandStick(
//            "rotate_y", "rotate_portal_rotation_along y 15"
//        );
//        registerPortalSubCommandStick(
//            "rotate_z", "rotate_portal_rotation_along z 15"
//        );
//        registerPortalSubCommandStick(
//            "make_unbreakable", "nbt {unbreakable:true}"
//        );
//        registerPortalSubCommandStick(
//            "make_fuse_view", "nbt {fuseView:true}"
//        );
//        registerPortalSubCommandStick(
//            "enable_pos_adjust", "nbt {adjustPositionAfterTeleport:true}"
//        );
//        registerPortalSubCommandStick(
//            "disable_rendering_yourself", "nbt {doRenderPlayer:false}"
//        );
//        registerPortalSubCommandStick(
//            "enable_isometric", "debug isometric_enable 50"
//        );
//        registerPortalSubCommandStick(
//            "disable_isometric", "debug isometric_disable"
//        );
//        registerPortalSubCommandStick(
//            "create_5_connected_rooms", "create_connected_rooms roomSize 6 4 6 roomNumber 5"
//        );
//        registerPortalSubCommandStick(
//            "accelerate50", "debug accelerate 50"
//        );
//        registerPortalSubCommandStick(
//            "accelerate200", "debug accelerate 200"
//        );
//        registerPortalSubCommandStick(
//            "reverse_accelerate50", "debug accelerate -50"
//        );
//        registerPortalSubCommandStick(
//            "enable_gravity_change", "nbt {teleportChangesGravity:true}"
//        );
//        registerPortalSubCommandStick(
//            "make_invisible", "nbt {isVisible:false}"
//        );
//        registerPortalSubCommandStick(
//            "make_visible", "nbt {isVisible:true}"
//        );
//        registerPortalSubCommandStick(
//            "disable_default_animation", "nbt {defaultAnimation:{durationTicks:0}}"
//        );
//
//        registerPortalSubCommandStick(
//            "pause_animation", "animation pause"
//        );
//        registerPortalSubCommandStick(
//            "resume_animation", "animation resume"
//        );
//
//        registerPortalSubCommandStick(
//            "rotate_around_y", "animation rotate_infinitely @s 0 1 0 1.0"
//        );
//        registerPortalSubCommandStick(
//            "rotate_randomly", "animation rotate_infinitely_random"
//        );
//        CommandStickItem.registerType(
//            "imm_ptl:rotate_around_view",
//            new CommandStickItem.Data(
//                "execute positioned 0.0 0.0 0.0 run portal animation rotate_infinitely @p ^0.0 ^0.0 ^1.0 1.7",
//                "imm_ptl.command.rotate_around_view",
//                Lists.newArrayList("imm_ptl.command_dest.rotate_around_view")
//            )
//        );
//        registerPortalSubCommandStick(
//            "expand_from_center", "animation expand_from_center 20"
//        );
//        registerPortalSubCommandStick(
//            "clear_animation", "animation clear"
//        );
//        CommandStickItem.registerType("imm_ptl:reset_scale", new CommandStickItem.Data(
//            "/scale set pehkui:base 1",
//            "imm_ptl.command.reset_scale",
//            Lists.newArrayList("imm_ptl.command_desc.reset_scale")
//        ));
//        CommandStickItem.registerType("imm_ptl:long_reach", new CommandStickItem.Data(
//            "/scale set pehkui:reach 5",
//            "imm_ptl.command.long_reach",
//            Lists.newArrayList("imm_ptl.command_desc.long_reach")
//        ));
//        CommandStickItem.registerType("imm_ptl:night_vision", new CommandStickItem.Data(
//            "/effect give @s minecraft:night_vision 9999 1 true",
//            "imm_ptl.command.night_vision",
//            List.of()
//        ));
        
//        registerPortalSubCommandStick(
//            "rotate_around_y", "animation rotate_infinitely @s 0 1 0 1.0"
//        );
//        registerPortalSubCommandStick(
//            "rotate_randomly", "animation rotate_infinitely_random"
//        );
        CommandStickItem.registerType(
            "imm_ptl:rotate_around_view",
            new CommandStickItem.Data(
                "execute positioned 0.0 0.0 0.0 run portal animation rotate_infinitely @p ^0.0 ^0.0 ^1.0 1.7",
                "imm_ptl.command.rotate_around_view",
                Lists.newArrayList("imm_ptl.command_dest.rotate_around_view"), true
            )
        );
        registerPortalSubCommandStick(
            "expand_from_center", "animation expand_from_center 20"
        );
        registerPortalSubCommandStick(
            "clear_animation", "animation clear"
        );
        CommandStickItem.registerType("imm_ptl:reset_scale", new CommandStickItem.Data(
            "/scale set pehkui:base 1",
            "imm_ptl.command.reset_scale",
            Lists.newArrayList("imm_ptl.command_desc.reset_scale"), true
        ));
        CommandStickItem.registerType("imm_ptl:long_reach", new CommandStickItem.Data(
            "/scale set pehkui:reach 5",
            "imm_ptl.command.long_reach",
            Lists.newArrayList("imm_ptl.command_desc.long_reach"), true
        ));
        CommandStickItem.registerType("imm_ptl:night_vision", new CommandStickItem.Data(
            "/effect give @s minecraft:night_vision 9999 1 true",
            "imm_ptl.command.night_vision",
            List.of(), true
        ));
        
//        registerPortalSubCommandStick(
//            "goback"
//        );
//        registerPortalSubCommandStick(
//            "show_wiki", "wiki"
//        );
    } // TODO @Nick1st fix this total mess I fabricated
    
    private static void registerPortalSubCommandStick(String name) {
        registerPortalSubCommandStick(name, name);
    }
    
    private static void registerPortalSubCommandStick(String name, String subCommand) {
        CommandStickItem.registerType("imm_ptl:" + name, new CommandStickItem.Data(
            "/portal " + subCommand,
            "imm_ptl.command." + name,
            Lists.newArrayList("imm_ptl.command_desc." + name), true
        ));
    }
    
//    public static class IndirectMerger {
//        private static final DoubleList EMPTY = DoubleLists.unmodifiable(DoubleArrayList.wrap(new double[]{0.0}));
//        private final double[] result;
//        private final int[] firstIndices;
//        private final int[] secondIndices;
//        private final int resultLength;
//
//        public IndirectMerger(DoubleList l1, DoubleList l2, boolean override1, boolean override2) {
//            double limit = Double.NaN;
//            int size1 = l1.size();
//            int size2 = l2.size();
//            int sumSize = size1 + size2;
//            this.result = new double[sumSize];
//            this.firstIndices = new int[sumSize];
//            this.secondIndices = new int[sumSize];
//            boolean skipEndpointsOf2 = !override1;
//            boolean skipEndpointsOf1 = !override2;
//            int resultIndex = 0;
//            int index1 = 0;
//            int index2 = 0;
//            while (true) {
//                boolean reachLimit1 = index1 >= size1;
//                boolean reachLimit2 = index2 >= size2;
//                if (reachLimit1 && reachLimit2) break;
//                boolean shouldMove1 = !reachLimit1 && (reachLimit2 || l1.getDouble(index1) < l2.getDouble(index2) + 1.0E-7);
//                if (shouldMove1) {
//                    ++index1;
//                    if (skipEndpointsOf2 && (index2 == 0 || reachLimit2)) {
//                        continue;
//                    }
//                } else {
//                    ++index2;
//                    if (skipEndpointsOf1 && (index1 == 0 || reachLimit1)) continue;
//                }
//                int lastIndex1 = index1 - 1;
//                int lastIndex2 = index2 - 1;
//                double number = shouldMove1 ? l1.getDouble(lastIndex1) : l2.getDouble(lastIndex2);
//                if (!(limit >= number - 1.0E-7)) {
//                    this.firstIndices[resultIndex] = lastIndex1;
//                    this.secondIndices[resultIndex] = lastIndex2;
//                    this.result[resultIndex] = number;
//                    ++resultIndex;
//                    limit = number;
//                    continue;
//                }
//                this.firstIndices[resultIndex - 1] = lastIndex1;
//                this.secondIndices[resultIndex - 1] = lastIndex2;
//            }
//            this.resultLength = Math.max(1, resultIndex);
//        }
//    }

*/
}
