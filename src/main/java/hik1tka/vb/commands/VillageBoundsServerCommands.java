package hik1tka.vb.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import hik1tka.vb.VillageState;
import hik1tka.vb.VillageData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class VillageBoundsServerCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("vb")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.literal("remove_territory")
                            // Виклик списку з пагінацією: /vb remove_territory [page]
                            .executes(context -> listVillages(context, 1))
                            .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                    .executes(context -> listVillages(context, IntegerArgumentType.getInteger(context, "page"))))
                            // Видалення
                            .then(CommandManager.argument("uuid", StringArgumentType.string()))
                    )
            );

        });
    }

    private static int listVillages(com.mojang.brigadier.context.CommandContext<net.minecraft.server.command.ServerCommandSource> context, int page) {
        VillageState state = VillageState.getServerState(context.getSource().getWorld().getServer().getOverworld());
        List<VillageData> allVillages = new ArrayList<>(state.getAllVillages());
        int pageSize = 9; // 9 рядків + 1 для кнопки "Далі"
        int maxPages = (int) Math.ceil((double) allVillages.size() / pageSize);

        if (allVillages.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("Список територій порожній."), false);
            return 1;
        }

        context.getSource().sendFeedback(() -> Text.literal("--- Список територій (Сторінка " + page + "/" + maxPages + ") ---").formatted(Formatting.GOLD), false);

        int start = (page - 1) * pageSize;
        for (int i = start; i < Math.min(start + pageSize, allVillages.size()); i++) {
            VillageData data = allVillages.get(i);
            String id = data.getVillageId();

            // Створюємо клікабельний текст ID
            Text idText = Text.literal(id).formatted(Formatting.GREEN)
                    .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Натисніть, щоб скопіювати ID"))));

            Text line = Text.literal("- ").append(idText)
                    .append(Text.literal(" | " + data.getName() + " | Власник: " + data.getOwnerName()).formatted(Formatting.WHITE));

            context.getSource().sendFeedback(() -> line, false);
        }

        // Відображення кнопки "Далі" якщо є наступна сторінка
        if (page < maxPages) {
            int nextPage = page + 1;
            Text nextBtn = Text.literal(">>> Наступна сторінка " + nextPage + " <<<")
                    .formatted(Formatting.GREEN, Formatting.BOLD)
                    .styled(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vb remove_territory " + nextPage))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Натисніть для переходу"))));
            context.getSource().sendFeedback(() -> nextBtn, false);
        }

        return 1;
    }
}