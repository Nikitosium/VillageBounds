package hik1tka.vb.item;

import hik1tka.vb.screen.DocumentFolderScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory; // Важливо!
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class DocumentFolderItem extends Item {

    public DocumentFolderItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        final ItemStack itemStack = user.getStackInHand(hand);

        if (!world.isClient) {
            // Використовуємо SimpleNamedScreenHandlerFactory
            user.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, player) -> new DocumentFolderScreenHandler(syncId, playerInventory, itemStack),
                    Text.translatable("screen.villagebounds.document_folder")
            ));
        }

        return TypedActionResult.success(itemStack, world.isClient());
    }
}