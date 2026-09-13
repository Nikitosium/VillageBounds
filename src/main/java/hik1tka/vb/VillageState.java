package hik1tka.vb;

import hik1tka.vb.item.SettlementProjectItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VillageState extends PersistentState {
    private final Map<String, VillageData> villages = new LinkedHashMap<>();

    public static VillageState fromNbt(NbtCompound nbt) {
        VillageState state = new VillageState();

        NbtList villagesList = nbt.getList("Villages", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < villagesList.size(); i++) {
            VillageData data = VillageData.fromNbt(villagesList.getCompound(i));
            state.villages.put(data.getVillageId(), data);
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList villagesList = new NbtList();

        for (VillageData data : villages.values()) {
            villagesList.add(data.toNbt());
        }

        nbt.put("Villages", villagesList);
        return nbt;
    }

    public static VillageState getServerState(ServerWorld world) {
        return world.getServer()
                .getOverworld()
                .getPersistentStateManager()
                .getOrCreate(VillageState::fromNbt, VillageState::new, "village_bounds_data");
    }

    public VillageData addVillage(
            String name,
            net.minecraft.util.math.BlockPos center,
            List<net.minecraft.util.math.BlockPos> points,
            String ownerUuid,
            String ownerName
    ) {
        VillageData data = VillageData.createNew(name, center, points, ownerUuid, ownerName);
        villages.put(data.getVillageId(), data);
        markDirty();
        return data;
    }

    public VillageData getVillageById(String villageId) {
        return villages.get(villageId);
    }

    public boolean isOriginalDocument(String villageId, String documentId) {
        VillageData data = villages.get(villageId);
        return data != null && data.isOriginalDocument(documentId);
    }

    public boolean renameVillage(String villageId, String newName) {
        VillageData data = villages.get(villageId);
        if (data == null) {
            return false;
        }

        data.setName(newName);
        markDirty();
        return true;
    }

    public boolean replaceVillageGeometry(
            String villageId,
            net.minecraft.util.math.BlockPos newCenter,
            List<net.minecraft.util.math.BlockPos> newPoints
    ) {
        VillageData data = villages.get(villageId);
        if (data == null) {
            return false;
        }

        data.setCenter(newCenter);
        data.setPoints(newPoints);
        markDirty();
        return true;
    }

    public boolean transferOwnership(String villageId, String newOwnerUuid, String newOwnerName) {
        VillageData data = villages.get(villageId);
        if (data == null) {
            return false;
        }

        data.setOwnerUuid(newOwnerUuid);
        data.setOwnerName(newOwnerName);
        markDirty();
        return true;
    }

    public boolean removeVillage(String villageId) {
        VillageData removed = villages.remove(villageId);
        if (removed != null) {
            markDirty();
            return true;
        }
        return false;
    }

    public List<VillageData> getAllVillages() {
        return new ArrayList<>(villages.values());
    }

    public boolean isPositionProtected(net.minecraft.util.math.BlockPos pos, double outerBuffer) {
        for (VillageData data : villages.values()) {
            if (data.getPoints() != null && data.getPoints().size() >= 3) {
                if (VillageGeometryUtil.isInsideOrNearPolygon(pos, data.getPoints(), outerBuffer)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeVarInt(villages.size());

        for (VillageData data : villages.values()) {
            buf.writeString(data.getVillageId(), 64);
            buf.writeString(data.getName(), 64);
            buf.writeBlockPos(data.getCenter());

            List<net.minecraft.util.math.BlockPos> points = data.getPoints();
            buf.writeVarInt(points.size());
            for (net.minecraft.util.math.BlockPos point : points) {
                buf.writeBlockPos(point);
            }
        }
    }

    public boolean canManageVillage(ServerPlayerEntity player, ItemStack stack) {
        if (!(stack.getItem() instanceof SettlementProjectItem)) {
            return false;
        }

        String villageId = SettlementProjectItem.getVillageId(stack);
        String documentId = SettlementProjectItem.getDocumentId(stack);
        String ownerUuidInDoc = SettlementProjectItem.getOwnerUuid(stack);

        if (villageId.isEmpty() || documentId.isEmpty() || ownerUuidInDoc.isEmpty()) {
            return false;
        }

        VillageData data = getVillageById(villageId);
        if (data == null) {
            return false;
        }

        if (!data.getOwnerUuid().equals(player.getUuidAsString())) {
            return false;
        }

        if (!data.getOwnerUuid().equals(ownerUuidInDoc)) {
            return false;
        }

        return data.isOriginalDocument(documentId);
    }
}