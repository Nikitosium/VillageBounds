package hik1tka.vb.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;

public class ClientMethods {

    public static void openNamingScreen(ItemStack stack) {
        MinecraftClient.getInstance().setScreen(new VillageNamingScreen(stack));
    }

    public static void openManageScreen(ItemStack stack) {
        MinecraftClient.getInstance().setScreen(new VillageManageScreen(stack));
    }

    public static void openRenameScreen(ItemStack stack) {
        MinecraftClient.getInstance().setScreen(new VillageRenameScreen(stack));
    }

    public static boolean isControlDown() {
        return Screen.hasControlDown();
    }
}