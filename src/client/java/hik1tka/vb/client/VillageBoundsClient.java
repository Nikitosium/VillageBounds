package hik1tka.vb.client;

import hik1tka.vb.VillageBounds;

import hik1tka.vb.client.render.VillageBoundsRender;
import hik1tka.vb.client.commands.VillageBoundsClientCommands;
import hik1tka.vb.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static hik1tka.vb.client.commands.VillageBoundsClientCommands.SHOW_BUFFER;

public class VillageBoundsClient implements ClientModInitializer {
    public static final List<ClientVillage> CLIENT_VILLAGES = new ArrayList<>();

    private static final String TAG_PROJECT_ACTIVE = "ProjectActive";
    private static final String TAG_PROJECT_USED = "ProjectUsed";
    private static final String TAG_VILLAGE_ID = "VillageId";

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(VillageBounds.SYNC_VILLAGES_ID, (client, handler, buf, responseSender) -> {
            List<ClientVillage> loadedVillages = readVillagesFromBuf(buf);

            client.execute(() -> {
                CLIENT_VILLAGES.clear();
                CLIENT_VILLAGES.addAll(loadedVillages);
            });
        });

        WorldRenderEvents.END.register(this::render);
        HudRenderCallback.EVENT.register(new InGameHudHandler());
        VillageBoundsClientCommands.registerCommands();

        // Підміна текстур балісти за матеріалом (BallistaDynamicBakedModel)
        ModelLoadingPlugin.register(new BallistaModelClient());

        // Гарантований ре-рендер конкретної позиції одразу після приходу коректних
        // даних матеріалу (див. BallistaBaseBlockEntity.CLIENT_RERENDER_HOOK) -
        // без цього нову основу могло "заклинити" на дубовій текстурі, доки чанк
        // не перебейкається з якоїсь іншої причини (напр. від сусідньої основи).
        hik1tka.vb.block.entity.BallistaBaseBlockEntity.CLIENT_RERENDER_HOOK = pos -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.worldRenderer != null) {
                mc.worldRenderer.scheduleBlockRenders(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            }
        };


        net.minecraft.client.gui.screen.ingame.HandledScreens.register(
                hik1tka.vb.screen.ModScreenHandlers.DOCUMENT_FOLDER_SCREEN_HANDLER,
                hik1tka.vb.client.screen.DocumentFolderScreen::new
        );
    }

    private static List<ClientVillage> readVillagesFromBuf(PacketByteBuf buf) {
        List<ClientVillage> villages = new ArrayList<>();

        int villageCount = buf.readVarInt();
        for (int i = 0; i < villageCount; i++) {
            String villageId = buf.readString(64);
            String name = buf.readString(64);
            BlockPos center = buf.readBlockPos();

            int pointCount = buf.readVarInt();
            List<BlockPos> points = new ArrayList<>();
            for (int j = 0; j < pointCount; j++) {
                points.add(buf.readBlockPos());
            }

            villages.add(new ClientVillage(villageId, name, center, points));
        }

        return villages;
    }

    private void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        ItemStack stack = client.player.getMainHandStack();
        boolean hasProjectInHand = stack.isOf(ModItems.SETTLEMENT_PROJECT);
        boolean isFolderScreenOpen = client.player.currentScreenHandler instanceof hik1tka.vb.screen.DocumentFolderScreenHandler;

        // 1. Екстрений вихід, якщо малювати нічого не треба
        if (!hasProjectInHand && !isFolderScreenOpen) {
            return;
        }

        NbtCompound nbt = hasProjectInHand ? stack.getNbt() : null;

        // 2. Викликаємо підготовку сцени й OpenGL налаштувань з нового класу
        Matrix4f matrix = VillageBoundsRender.beginRender(context, client);
        if (matrix == null) {
            return;
        }

        // 3. Викликаємо малювання ліжок, дзвонів та буферних зон
        VillageBoundsRender.renderSpecialBlocks(matrix, client);

        if (SHOW_BUFFER) {
            VillageBoundsRender.renderBufferZones(matrix);
        }

        // 4. Логіка рук та відображення проєктів/селищ
        if (hasProjectInHand && nbt != null) {
            if (nbt.getBoolean(TAG_PROJECT_ACTIVE)) {
                VillageBoundsRender.renderCurrentProject(matrix, stack);
            } else if (nbt.getBoolean(TAG_PROJECT_USED) && nbt.contains(TAG_VILLAGE_ID)) {
                String villageId = nbt.getString(TAG_VILLAGE_ID);
                ClientVillage village = findVillageById(villageId);
                if (village != null) {
                    VillageBoundsRender.renderSingleVillage(matrix, village);
                }
            }
        } else if (isFolderScreenOpen) {
            for (ClientVillage village : CLIENT_VILLAGES) {
                VillageBoundsRender.renderSingleVillage(matrix, village);
            }
        }

        // 5. Викликаємо очищення сцени
        VillageBoundsRender.endRender(context);
    }

    private ClientVillage findVillageById(String villageId) {
        for (ClientVillage village : CLIENT_VILLAGES) {
            if (village.villageId().equals(villageId)) {
                return village;
            }
        }
        return null;
    }

    public record ClientVillage(String villageId, String name, BlockPos center, List<BlockPos> points) {
    }
}