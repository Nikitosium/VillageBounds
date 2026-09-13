package hik1tka.vb.client;

import hik1tka.vb.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;

public class InGameHudHandler implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        ItemStack stack = client.player.getMainHandStack();
        if (!stack.isOf(ModItems.SETTLEMENT_PROJECT)) {
            return;
        }

        TextRenderer renderer = client.textRenderer;
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList points = nbt.getList("Points", NbtElement.COMPOUND_TYPE);

        Text villageName = nbt.contains("VillageName")
                ? Text.literal(nbt.getString("VillageName"))
                : Text.translatable("hud.villagebounds.unknown_name");

        int y = 10;
        drawContext.drawTextWithShadow(renderer, villageName, 10, y, 0xFFD54F);
        drawContext.drawTextWithShadow(renderer, Text.translatable("hud.villagebounds.points", points.size()), 10, y + 10, 0xFFFFFF);
        drawContext.drawTextWithShadow(renderer, Text.translatable("hud.villagebounds.villages", VillageBoundsClient.CLIENT_VILLAGES.size()), 10, y + 20, 0xFFFFFF);

        if (points.size() > 0) {
            drawContext.drawTextWithShadow(renderer, Text.translatable("hud.villagebounds.finish_hint"), 10, y + 35, 0xAAAAAA);
        }
    }
}