package hik1tka.vb.registry;

import hik1tka.vb.VillageBounds;
import hik1tka.vb.item.SettlementProjectItem; // ПЕРЕВІР ЦЕЙ ІМПОРТ
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import hik1tka.vb.item.DocumentFolderItem;


public class ModItems {

    // ВАЖЛИВО: Використовуємо твій клас SettlementProjectItem замість звичайного Item
    public static final Item SETTLEMENT_PROJECT = new SettlementProjectItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item DOCUMENT_FOLDER = new DocumentFolderItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item PORTFOLIO = new Item(new Item.Settings().maxCount(1).rarity(Rarity.RARE));
    public static final Item COOPER_COIN = new Item(new Item.Settings().maxCount(100).rarity(Rarity.COMMON));
    public static final Item SILVER_COIN = new Item(new Item.Settings().maxCount(100).rarity(Rarity.RARE));
    public static final Item GOLD_COIN = new Item(new Item.Settings().maxCount(100).rarity(Rarity.UNCOMMON));


    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(VillageBounds.MOD_ID, "settlement_project"), SETTLEMENT_PROJECT);
        Registry.register(Registries.ITEM, new Identifier(VillageBounds.MOD_ID, "document_folder"),DOCUMENT_FOLDER);
        Registry.register(Registries.ITEM, new Identifier(VillageBounds.MOD_ID, "portfolio"),PORTFOLIO);
        Registry.register(Registries.ITEM, new Identifier(VillageBounds.MOD_ID,"cooper_coin"),COOPER_COIN);
        Registry.register(Registries.ITEM, new Identifier(VillageBounds.MOD_ID, "silver_coin"), SILVER_COIN);
        Registry.register(Registries.ITEM, new Identifier(VillageBounds.MOD_ID, "gold_coin"),GOLD_COIN);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SETTLEMENT_PROJECT);
            entries.add(DOCUMENT_FOLDER);
            entries.add(PORTFOLIO);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(COOPER_COIN);
            entries.add(SILVER_COIN);
            entries.add(GOLD_COIN);
        });
    }



}

