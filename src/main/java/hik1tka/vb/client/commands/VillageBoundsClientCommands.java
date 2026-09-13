package hik1tka.vb.client.commands;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class VillageBoundsClientCommands {
    // Зробили public static, щоб можна було зчитувати стан з інших файлів
    public static boolean SHOW_BUFFER = false;

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("vb_show_buffer")
                    .executes(context -> toggleBufferVisibility()));
        });
    }

    public static int toggleBufferVisibility() {
        SHOW_BUFFER = !SHOW_BUFFER;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                    Text.literal("Village buffer render: " + (SHOW_BUFFER ? "ON" : "OFF")),
                    false
            );
        }
        return 1;
    }
}