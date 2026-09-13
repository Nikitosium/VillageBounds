package hik1tka.vb.client;

import hik1tka.vb.VillageBounds;
import hik1tka.vb.item.SettlementProjectItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class VillageManageScreen extends Screen {
    private final ItemStack stack;

    public VillageManageScreen(ItemStack stack) {
        super(Text.translatable("screen.villagebounds.manage.title"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 55;
        int buttonWidth = 170;
        int buttonHeight = 20;
        int gap = 24;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.villagebounds.manage.edit_borders"),
                button -> {
                    ClientPlayNetworking.send(VillageBounds.BEGIN_EDIT_BORDERS_ID, PacketByteBufs.create());
                    this.close();
                }
        ).dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.villagebounds.manage.rename"),
                button -> ClientMethods.openRenameScreen(stack)
        ).dimensions(centerX - buttonWidth / 2, startY + gap, buttonWidth, buttonHeight).build());


    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        String villageId = SettlementProjectItem.getVillageId(stack);
        String ownerName = SettlementProjectItem.getOwnerName(stack);
        String documentId = SettlementProjectItem.getDocumentId(stack);

        int centerX = this.width / 2;

        // Верхній блок тексту
        int titleY = this.height / 2 - 110;
        int infoY1 = titleY + 18;
        int infoY2 = titleY + 32;
        int infoY3 = titleY + 46;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, titleY, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.villagebounds.manage.village_id", villageId),
                centerX, infoY1, 0xAAAAAA);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.villagebounds.manage.owner", ownerName),
                centerX, infoY2, 0xAAAAAA);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.villagebounds.manage.document", shortId(documentId)),
                centerX, infoY3, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    private String shortId(String id) {
        if (id == null || id.isEmpty()) {
            return "-";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }
}