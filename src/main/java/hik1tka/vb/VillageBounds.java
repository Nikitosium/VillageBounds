package hik1tka.vb;

import hik1tka.vb.item.SettlementProjectItem;
import hik1tka.vb.registry.ModBlockEntities;
import hik1tka.vb.registry.ModBlocks;
import hik1tka.vb.registry.ModItems;
import hik1tka.vb.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageBounds implements ModInitializer {
    public static final String MOD_ID = "villagebounds";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier SET_VILLAGE_NAME_ID = new Identifier(MOD_ID, "set_village_name");
    public static final Identifier SYNC_VILLAGES_ID = new Identifier(MOD_ID, "sync_villages");

    public static final Identifier RENAME_VILLAGE_ID = new Identifier(MOD_ID, "rename_village");
    public static final Identifier BEGIN_EDIT_BORDERS_ID = new Identifier(MOD_ID, "begin_edit_borders");

    public static final Block ANCHOR_BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(MOD_ID, "anchor"),
            new Block(FabricBlockSettings.copyOf(Blocks.STRUCTURE_VOID).noCollision().nonOpaque()) {
                @Override
                public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
                    return VoxelShapes.empty();
                }
            }
    );

    @Override
    public void onInitialize() {
        ModItems.register();
        ModScreenHandlers.registerScreenHandlers();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerModBlockEntities();

        ServerPlayNetworking.registerGlobalReceiver(SET_VILLAGE_NAME_ID, (server, player, handler, buf, responseSender) -> {
            String name = buf.readString(64);

            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof SettlementProjectItem) {
                    SettlementProjectItem.setName(stack, name, player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RENAME_VILLAGE_ID, (server, player, handler, buf, responseSender) -> {
            String newName = buf.readString(64);

            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!SettlementProjectItem.canManage(player, stack)) {
                    player.sendMessage(Text.translatable("message.villagebounds.manage_denied"), true);
                    return;
                }

                VillageState state = VillageState.getServerState(player.getServerWorld());
                String villageId = SettlementProjectItem.getVillageId(stack);

                if (!state.renameVillage(villageId, newName)) {
                    player.sendMessage(Text.translatable("message.villagebounds.rename_failed"), true);
                    return;
                }

                VillageData data = state.getVillageById(villageId);
                if (data == null) {
                    player.sendMessage(Text.translatable("message.villagebounds.rename_failed"), true);
                    return;
                }

                SettlementProjectItem.linkDocument(stack, data, SettlementProjectItem.getDocumentId(stack));
                player.sendMessage(Text.translatable("message.villagebounds.rename_success", newName), true);
                syncVillagesToPlayer(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(BEGIN_EDIT_BORDERS_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (!SettlementProjectItem.canManage(player, stack)) {
                    player.sendMessage(Text.translatable("message.villagebounds.manage_denied"), true);
                    return;
                }

                VillageState state = VillageState.getServerState(player.getServerWorld());
                String villageId = SettlementProjectItem.getVillageId(stack);
                VillageData data = state.getVillageById(villageId);

                if (data == null) {
                    player.sendMessage(Text.translatable("message.villagebounds.document_invalid"), true);
                    return;
                }

                SettlementProjectItem.beginEditOnDocument(stack, data, SettlementProjectItem.getDocumentId(stack));
                player.sendMessage(Text.translatable("message.villagebounds.edit_mode_started"), true);
            });
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncVillagesToPlayer(handler.player));

        LOGGER.info("VillageBounds initialized");

        hik1tka.vb.commands.VillageBoundsServerCommands.register();
    }

    public static void syncVillagesToPlayer(ServerPlayerEntity player) {
        VillageState state = VillageState.getServerState(player.getServerWorld());

        PacketByteBuf buf = PacketByteBufs.create();
        state.writeToBuf(buf);

        ServerPlayNetworking.send(player, SYNC_VILLAGES_ID, buf);
    }
}