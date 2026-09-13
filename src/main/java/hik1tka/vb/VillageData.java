package hik1tka.vb;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VillageData {
    private final String villageId;
    private String name;
    private BlockPos center;
    private List<BlockPos> points;

    private String ownerUuid;
    private String ownerName;

    private String originalDocumentId;

    public VillageData(
            String villageId,
            String name,
            BlockPos center,
            List<BlockPos> points,
            String ownerUuid,
            String ownerName,
            String originalDocumentId
    ) {
        this.villageId = villageId;
        this.name = name;
        this.center = center;
        this.points = new ArrayList<>(points);
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.originalDocumentId = originalDocumentId;
    }

    public static VillageData createNew(
            String name,
            BlockPos center,
            List<BlockPos> points,
            String ownerUuid,
            String ownerName
    ) {
        return new VillageData(
                UUID.randomUUID().toString(),
                name,
                center,
                points,
                ownerUuid,
                ownerName,
                UUID.randomUUID().toString()
        );
    }

    public String getVillageId() {
        return villageId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BlockPos getCenter() {
        return center;
    }

    public void setCenter(BlockPos center) {
        this.center = center;
    }

    public List<BlockPos> getPoints() {
        return points;
    }

    public void setPoints(List<BlockPos> points) {
        this.points = new ArrayList<>(points);
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOriginalDocumentId() {
        return originalDocumentId;
    }

    public boolean isOriginalDocument(String documentId) {
        return originalDocumentId != null
                && !originalDocumentId.isEmpty()
                && originalDocumentId.equals(documentId);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("VillageId", villageId);
        nbt.putString("Name", name);
        nbt.put("Center", NbtHelper.fromBlockPos(center));
        nbt.putString("OwnerUuid", ownerUuid);
        nbt.putString("OwnerName", ownerName);
        nbt.putString("OriginalDocumentId", originalDocumentId == null ? "" : originalDocumentId);

        NbtList pointsList = new NbtList();
        for (BlockPos point : points) {
            pointsList.add(NbtHelper.fromBlockPos(point));
        }
        nbt.put("Points", pointsList);

        return nbt;
    }

    public static VillageData fromNbt(NbtCompound nbt) {
        String villageId = nbt.getString("VillageId");
        String name = nbt.getString("Name");
        BlockPos center = NbtHelper.toBlockPos(nbt.getCompound("Center"));
        String ownerUuid = nbt.getString("OwnerUuid");
        String ownerName = nbt.getString("OwnerName");
        String originalDocumentId = nbt.getString("OriginalDocumentId");

        List<BlockPos> points = new ArrayList<>();
        NbtList pointsList = nbt.getList("Points", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < pointsList.size(); i++) {
            points.add(NbtHelper.toBlockPos(pointsList.getCompound(i)));
        }

        VillageData data = new VillageData(
                villageId,
                name,
                center,
                points,
                ownerUuid,
                ownerName,
                originalDocumentId
        );

        return data;
    }
}