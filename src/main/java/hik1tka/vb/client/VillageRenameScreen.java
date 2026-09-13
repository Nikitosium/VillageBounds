package hik1tka.vb.client;

import hik1tka.vb.VillageBounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class VillageRenameScreen extends Screen {
    private final ItemStack stack;
    private TextFieldWidget nameField;
    private ButtonWidget acceptButton;

    public VillageRenameScreen(ItemStack stack) {
        super(Text.translatable("screen.villagebounds.rename.title"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameField = new TextFieldWidget(
                this.textRenderer,
                centerX - 100,
                centerY - 20,
                200,
                20,
                Text.translatable("screen.villagebounds.rename.input")
        );
        this.nameField.setMaxLength(64);
        if (stack.hasCustomName()) {
            this.nameField.setText(stack.getName().getString());
        }

        this.addSelectableChild(this.nameField);
        this.addDrawableChild(this.nameField);
        this.setInitialFocus(this.nameField);

        this.acceptButton = ButtonWidget.builder(
                Text.translatable("screen.villagebounds.rename.accept"),
                button -> confirm()
        ).dimensions(centerX - 105, centerY + 10, 100, 20).build();

        ButtonWidget cancelButton = ButtonWidget.builder(
                Text.translatable("screen.villagebounds.rename.cancel"),
                button -> this.close()
        ).dimensions(centerX + 5, centerY + 10, 100, 20).build();

        this.addDrawableChild(this.acceptButton);
        this.addDrawableChild(cancelButton);

        updateButtonState();
    }

    @Override
    public void tick() {
        this.nameField.tick();
        updateButtonState();
    }

    private void updateButtonState() {
        this.acceptButton.active = this.nameField.getText().trim().length() >= 3;
    }

    private void confirm() {
        String newName = this.nameField.getText().trim();

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(newName);

        ClientPlayNetworking.send(VillageBounds.RENAME_VILLAGE_ID, buf);
        this.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER && this.acceptButton.active) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}