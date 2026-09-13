package hik1tka.vb.item;

import hik1tka.vb.VillageBounds;
import hik1tka.vb.VillageData;
import hik1tka.vb.VillageState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SettlementProjectItem extends Item {
    private static final String TAG_PROJECT_ACTIVE = "ProjectActive";
    private static final String TAG_PROJECT_USED = "ProjectUsed";

    private static final String TAG_VILLAGE_CENTER = "VillageCenter";
    private static final String TAG_VILLAGE_NAME = "VillageName";
    private static final String TAG_POINTS = "Points";

    private static final String TAG_REGISTERED_VILLAGE_NAME = "RegisteredVillageName";

    private static final String TAG_VILLAGE_ID = "VillageId";
    private static final String TAG_DOCUMENT_ID = "DocumentId";
    private static final String TAG_OWNER_UUID = "OwnerUuid";
    private static final String TAG_OWNER_NAME = "OwnerName";

    public SettlementProjectItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (player == null) {
            return ActionResult.PASS;
        }

        NbtCompound nbt = stack.getOrCreateNbt();

        if (isProjectUsed(nbt)) {
            if (!world.isClient) {
                player.sendMessage(Text.translatable("message.villagebounds.project_already_used"), true);
            }
            return ActionResult.FAIL;
        }

        // Клік по дзвону = старт проєкту / вибір центра
        if (world.getBlockState(pos).isOf(Blocks.BELL)) {
            if (isProjectActive(nbt)) {
                if (!world.isClient) {
                    player.sendMessage(Text.translatable("message.villagebounds.project_already_exists"), true);
                }
                return ActionResult.FAIL;
            }

            if (world.isClient) {
                openNamingScreenClient(stack);
            } else {
                nbt.put(TAG_VILLAGE_CENTER, NbtHelper.fromBlockPos(pos));
                nbt.putBoolean(TAG_PROJECT_ACTIVE, true);
                player.sendMessage(Text.translatable("message.villagebounds.center_set"), true);
            }

            return ActionResult.SUCCESS;
        }

        // Інакше це додавання точки
        if (!isProjectActive(nbt)) {
            if (!world.isClient) {
                player.sendMessage(Text.translatable("message.villagebounds.no_active_project"), true);
            }
            return ActionResult.FAIL;
        }

        if (!world.isClient) {
            addPoint(world, stack, pos.up(), player);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Відкриття GUI керування: Ctrl + ПКМ у повітря
        if (world.isClient) {
            if (isControlDownClient() && isLinkedVillageDocument(stack)) {
                openManageScreenClient(stack);
                return TypedActionResult.success(stack);
            }
        }

        // Фіналізація: Shift + ПКМ у повітря
        if (!world.isClient && user.isSneaking()) {
            NbtCompound nbt = stack.getOrCreateNbt();

            if (isProjectUsed(nbt)) {
                user.sendMessage(Text.translatable("message.villagebounds.project_already_used"), true);
                return TypedActionResult.fail(stack);
            }

            if (!isProjectActive(nbt)) {
                user.sendMessage(Text.translatable("message.villagebounds.no_active_project"), true);
                return TypedActionResult.fail(stack);
            }

            if (!nbt.contains(TAG_VILLAGE_NAME) || !nbt.contains(TAG_VILLAGE_CENTER)) {
                user.sendMessage(Text.translatable("message.villagebounds.save_error"), true);
                return TypedActionResult.fail(stack);
            }

            NbtList pointsNbt = nbt.getList(TAG_POINTS, NbtElement.COMPOUND_TYPE);
            if (pointsNbt.size() < 3) {
                user.sendMessage(Text.translatable("message.villagebounds.save_error"), true);
                return TypedActionResult.fail(stack);
            }

            String name = nbt.getString(TAG_VILLAGE_NAME);
            BlockPos center = NbtHelper.toBlockPos(nbt.getCompound(TAG_VILLAGE_CENTER));

            List<BlockPos> points = new ArrayList<>();
            for (int i = 0; i < pointsNbt.size(); i++) {
                points.add(NbtHelper.toBlockPos(pointsNbt.getCompound(i)));
            }

            VillageState state = VillageState.getServerState((ServerWorld) world);

            // soft edit: якщо документ уже був прив’язаний до села — не створюємо нове, а замінюємо геометрію
            String existingVillageId = getVillageId(stack);
            String existingDocumentId = getDocumentId(stack);

            if (!existingVillageId.isEmpty() && !existingDocumentId.isEmpty()) {
                if (!state.replaceVillageGeometry(existingVillageId, center, points)) {
                    user.sendMessage(Text.translatable("message.villagebounds.edit_save_failed"), true);
                    return TypedActionResult.fail(stack);
                }

                VillageData updated = state.getVillageById(existingVillageId);
                if (updated == null) {
                    user.sendMessage(Text.translatable("message.villagebounds.edit_save_failed"), true);
                    return TypedActionResult.fail(stack);
                }

                linkDocument(stack, updated, existingDocumentId);
                user.sendMessage(Text.translatable("message.villagebounds.edit_save_success"), true);

                if (user instanceof ServerPlayerEntity serverPlayer) {
                    VillageBounds.syncVillagesToPlayer(serverPlayer);
                }

                return TypedActionResult.success(stack);
            }

            // нове село
            VillageData data = state.addVillage(
                    name,
                    center,
                    points,
                    user.getUuidAsString(),
                    user.getName().getString()
            );

            linkDocument(stack, data, data.getOriginalDocumentId());
            user.sendMessage(Text.translatable("message.villagebounds.saved", name), false);

            if (user instanceof ServerPlayerEntity serverPlayer) {
                VillageBounds.syncVillagesToPlayer(serverPlayer);
            }

            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    private void addPoint(World world, ItemStack stack, BlockPos pos, PlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();

        if (isProjectUsed(nbt)) {
            player.sendMessage(Text.translatable("message.villagebounds.project_already_used"), true);
            return;
        }

        if (!isProjectActive(nbt)) {
            player.sendMessage(Text.translatable("message.villagebounds.no_active_project"), true);
            return;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        // Обмеження: новий якір не далі 24 блоків від POI
        if (!isNearPoi(serverWorld, pos, 48)) {
            player.sendMessage(Text.translatable("message.villagebounds.too_far_from_poi"), true);
            return;
        }

        NbtList points = nbt.getList(TAG_POINTS, NbtElement.COMPOUND_TYPE);

        // Обмеження: сегмент між останньою точкою і новою <= 32 блоки
        if (!points.isEmpty()) {
            BlockPos lastPoint = NbtHelper.toBlockPos(points.getCompound(points.size() - 1));
            if (lastPoint.getSquaredDistance(pos) > 64 * 64) {
                player.sendMessage(Text.translatable("message.villagebounds.segment_too_long"), true);
                return;
            }
        }

        if (world.getBlockState(pos).isAir()) {
            world.setBlockState(pos, VillageBounds.ANCHOR_BLOCK.getDefaultState());
        }

        points.add(NbtHelper.fromBlockPos(pos));
        nbt.put(TAG_POINTS, points);

        player.sendMessage(Text.translatable("message.villagebounds.point_added", points.size()), true);
    }

    private boolean isNearPoi(ServerWorld world, BlockPos pos, int maxDistance) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int dx = -maxDistance; dx <= maxDistance; dx++) {
            for (int dy = -maxDistance; dy <= maxDistance; dy++) {
                for (int dz = -maxDistance; dz <= maxDistance; dz++) {
                    mutable.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);

                    if (PointOfInterestTypes.getTypeForState(world.getBlockState(mutable)).isPresent()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static void setName(ItemStack stack, String name, PlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();

        if (isProjectUsed(nbt)) {
            if (!player.getWorld().isClient) {
                player.sendMessage(Text.translatable("message.villagebounds.project_already_used"), true);
            }
            return;
        }

        if (!isProjectActive(nbt)) {
            if (!player.getWorld().isClient) {
                player.sendMessage(Text.translatable("message.villagebounds.no_active_project"), true);
            }
            return;
        }

        nbt.putString(TAG_VILLAGE_NAME, name);

        if (!player.getWorld().isClient) {
            player.sendMessage(Text.translatable("message.villagebounds.name_accepted", name), true);
        }
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return true;
    }

    private static boolean isProjectActive(NbtCompound nbt) {
        return nbt != null && nbt.getBoolean(TAG_PROJECT_ACTIVE);
    }

    private static boolean isProjectUsed(NbtCompound nbt) {
        return nbt != null && nbt.getBoolean(TAG_PROJECT_USED);
    }

    public static void linkDocument(ItemStack stack, VillageData data, String documentId) {
        NbtCompound clean = new NbtCompound();
        clean.putBoolean(TAG_PROJECT_USED, true);
        clean.putBoolean(TAG_PROJECT_ACTIVE, false);

        clean.putString(TAG_REGISTERED_VILLAGE_NAME, data.getName());
        clean.putString(TAG_VILLAGE_ID, data.getVillageId());
        clean.putString(TAG_DOCUMENT_ID, documentId);
        clean.putString(TAG_OWNER_UUID, data.getOwnerUuid());
        clean.putString(TAG_OWNER_NAME, data.getOwnerName());

        stack.setNbt(clean);
        stack.setCustomName(Text.literal(data.getName()));
    }

    public static void beginEditOnDocument(ItemStack stack, VillageData data, String documentId) {
        NbtCompound nbt = new NbtCompound();

        nbt.putBoolean(TAG_PROJECT_ACTIVE, true);
        nbt.putBoolean(TAG_PROJECT_USED, false);

        nbt.putString(TAG_VILLAGE_NAME, data.getName());
        nbt.put(TAG_VILLAGE_CENTER, NbtHelper.fromBlockPos(data.getCenter()));
        nbt.put(TAG_POINTS, new NbtList());

        nbt.putString(TAG_VILLAGE_ID, data.getVillageId());
        nbt.putString(TAG_DOCUMENT_ID, documentId);
        nbt.putString(TAG_OWNER_UUID, data.getOwnerUuid());
        nbt.putString(TAG_OWNER_NAME, data.getOwnerName());

        stack.setNbt(nbt);
        stack.setCustomName(Text.literal(data.getName()));
    }

    public static String getVillageId(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(TAG_VILLAGE_ID) : "";
    }

    public static String getDocumentId(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(TAG_DOCUMENT_ID) : "";
    }

    public static String getOwnerUuid(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(TAG_OWNER_UUID) : "";
    }

    public static String getOwnerName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getString(TAG_OWNER_NAME) : "";
    }

    public static boolean isLinkedVillageDocument(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null
                && nbt.getBoolean(TAG_PROJECT_USED)
                && nbt.contains(TAG_VILLAGE_ID)
                && nbt.contains(TAG_DOCUMENT_ID);
    }

    public static boolean canManage(ServerPlayerEntity player, ItemStack stack) {
        if (!isLinkedVillageDocument(stack)) {
            return false;
        }

        VillageState state = VillageState.getServerState(player.getServerWorld());
        return state.canManageVillage(player, stack);
    }

    private static void openNamingScreenClient(ItemStack stack) {
        try {
            Class<?> clazz = Class.forName("hik1tka.vb.client.ClientMethods");
            Method method = clazz.getMethod("openNamingScreen", ItemStack.class);
            method.invoke(null, stack);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openManageScreenClient(ItemStack stack) {
        try {
            Class<?> clazz = Class.forName("hik1tka.vb.client.ClientMethods");
            Method method = clazz.getMethod("openManageScreen", ItemStack.class);
            method.invoke(null, stack);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isControlDownClient() {
        try {
            Class<?> clazz = Class.forName("hik1tka.vb.client.ClientMethods");
            Method method = clazz.getMethod("isControlDown");
            Object result = method.invoke(null);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }
}