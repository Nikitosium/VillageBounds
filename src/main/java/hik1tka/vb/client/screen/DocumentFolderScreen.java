package hik1tka.vb.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import hik1tka.vb.screen.DocumentFolderScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DocumentFolderScreen extends HandledScreen<DocumentFolderScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");
    private static final int ROWS = 4;

    public DocumentFolderScreen(DocumentFolderScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 114 + ROWS * 18;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 🔥 ФІКС: Передаємо тільки context, як вимагає твій компілятор
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, 89);
        context.drawTexture(TEXTURE, x, y + 89, 0, 125, this.backgroundWidth, 97);

        context.fill(x + 7, y + 17, x + 169, y + 180, 0xFFC6C6C6);

        // 1. Рендеримо слоти папки з твого динамічного циклу
        for (int i = 0; i < 27; i++) {
            Slot slot = this.handler.getSlot(i);
            if (slot.x != -999 && slot.y != -999) {
                int slotX = x + slot.x - 1;
                int slotY = y + slot.y - 1;
                // Вирізаємо одиночну рамку 18х18 пікселів
                context.drawTexture(TEXTURE, slotX, slotY, 7, 17, 18, 18);
            }
        }

        // 2. Рендеримо рамки для нижнього інвентарю гравця (всього 36 слотів: 27 сумка + 9 хотбар)
        // Починаємо з 27-го слоту в хендлері, бо перші 27 йшли під папку
        for (int i = 27; i < this.handler.slots.size(); i++) {
            Slot slot = this.handler.getSlot(i);
            int slotX = x + slot.x - 1;
            int slotY = y + slot.y - 1;
            context.drawTexture(TEXTURE, slotX, slotY, 7, 17, 18, 18);
        }
    }
}