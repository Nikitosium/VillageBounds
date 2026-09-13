package hik1tka.vb.mixin;

import hik1tka.vb.VillageBounds;
import hik1tka.vb.VillageState;
import hik1tka.vb.item.SettlementProjectItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends net.minecraft.entity.Entity {
    @Unique
    private boolean villagebounds$alreadyProcessed = false;

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }



    @Inject(method = "tick", at = @At("TAIL"))
    private void villagebounds$destroyVillageOnTickChecks(CallbackInfo ci) {
        if (this.getWorld().isClient()) {
            return;
        }

        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getStack();

        if (!SettlementProjectItem.isLinkedVillageDocument(stack)) {
            return;
        }

        // 1. ПЕРЕВІРКА НА ЗНИЩЕННЯ (Лава, вогонь, кактус тощо)
        // Якщо гра на цьому тіку видалила предмет (його хп закінчилися)
        if (self.isRemoved()) {
            villagebounds$destroyVillage(serverWorld, stack);
            return;
        }

        // 2. ПЕРЕВІРКА НА ДЕСПАВН (5 хвилин на землі)
        if (self.age >= 5999) {
            villagebounds$destroyVillage(serverWorld, stack);
            return;
        }

        // 3. ПЕРЕВІРКА НА ПАДІННЯ В БЕЗОДНЮ (Void)
        if (self.getY() < serverWorld.getBottomY() - 64) {
            villagebounds$destroyVillage(serverWorld, stack);
        }
    }

    @Unique
    private void villagebounds$destroyVillage(ServerWorld serverWorld, ItemStack stack) {
        if (villagebounds$alreadyProcessed) {
            return;
        }

        String villageId = SettlementProjectItem.getVillageId(stack);
        String documentId = SettlementProjectItem.getDocumentId(stack);

        if (villageId.isEmpty() || documentId.isEmpty()) {
            return;
        }

        villagebounds$alreadyProcessed = true;

        // ВИКЛИК ВИДАЛЕННЯ: Стираємо село з пам'яті та файлу збереження
        hik1tka.vb.VillageState state = hik1tka.vb.VillageState.getServerState(serverWorld);
        state.removeVillage(villageId);

        VillageBounds.LOGGER.info(
                "Village {} removed because linked document {} was destroyed in world",
                villageId,
                documentId
        );

        for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()) {
            VillageBounds.syncVillagesToPlayer(player);
        }
    }
}