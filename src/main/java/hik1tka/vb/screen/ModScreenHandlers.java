package hik1tka.vb.screen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public class ModScreenHandlers {

    public static final ScreenHandlerType<DocumentFolderScreenHandler> DOCUMENT_FOLDER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER,
                    new Identifier("villagebounds", "document_folder"),
                    new ScreenHandlerType<>(DocumentFolderScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    public static void registerScreenHandlers() {
        // Метод викликається в головному класі для ініціалізації статичних полів
    }
}