/*
 * GNU GENERAL PUBLIC LICENSE Version 3
 */
package drzhark.mocreatures.network.message;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.client.gui.MoCGUITamersNotebook;
import drzhark.mocreatures.entity.tameable.IMoCTameable;
import drzhark.mocreatures.entity.tameable.MoCPetData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class MoCMessageTamersNotebook {

    public static final class PetEntry {
        public final String name;
        public final String typeDisplay;
        public final String dimension;
        public final int blockX;
        public final int blockY;
        public final int blockZ;
        public final boolean inAmulet;

        public PetEntry(String name, String typeDisplay, String dimension, int blockX, int blockY, int blockZ, boolean inAmulet) {
            this.name = name != null ? name : "";
            this.typeDisplay = typeDisplay != null ? typeDisplay : "";
            this.dimension = dimension != null ? dimension : "";
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            this.inAmulet = inAmulet;
        }
    }

    private final List<PetEntry> entries;

    public MoCMessageTamersNotebook(List<PetEntry> entries) {
        this.entries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entries.size());
        for (PetEntry e : entries) {
            buffer.writeUtf(e.name, 256);
            buffer.writeUtf(e.typeDisplay, 256);
            buffer.writeUtf(e.dimension, 256);
            buffer.writeInt(e.blockX);
            buffer.writeInt(e.blockY);
            buffer.writeInt(e.blockZ);
            buffer.writeBoolean(e.inAmulet);
        }
    }

    public MoCMessageTamersNotebook(FriendlyByteBuf buffer) {
        this.entries = new ArrayList<>();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            String name = buffer.readUtf(256);
            String typeDisplay = buffer.readUtf(256);
            String dimension = buffer.readUtf(256);
            int x = buffer.readInt();
            int y = buffer.readInt();
            int z = buffer.readInt();
            boolean inAmulet = buffer.readBoolean();
            entries.add(new PetEntry(name, typeDisplay, dimension, x, y, z, inAmulet));
        }
    }

    public List<PetEntry> getEntries() {
        return entries;
    }

    /**
     * Builds the notebook payload from the server player's tamed creatures.
     */
    public static MoCMessageTamersNotebook create(ServerPlayer player) {
        if (player == null || MoCreatures.instance.mapData == null) {
            return new MoCMessageTamersNotebook(new ArrayList<>());
        }
        UUID ownerId = player.getUUID();
        List<PetEntry> list = new ArrayList<>();
        List<Integer> foundIds = new ArrayList<>();

        // Loaded entities: use live name, type, position
        for (ServerLevel level : player.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof IMoCTameable tameable
                        && ownerId.equals(tameable.getOwnerId())
                        && entity instanceof Mob mob) {
                    foundIds.add(tameable.getOwnerPetId());
                    String name = tameable.getPetName();
                    if (name == null) name = "";
                    String typeDisplay = mob.getType().getDescription().getString();
                    String dimension = level.dimension().location().toString();
                    int x = (int) Math.floor(entity.getX());
                    int y = (int) Math.floor(entity.getY());
                    int z = (int) Math.floor(entity.getZ());
                    list.add(new PetEntry(name, typeDisplay, dimension, x, y, z, false));
                }
            }
        }

        // Unloaded: from mapData
        MoCPetData petData = MoCreatures.instance.mapData.getPetData(ownerId);
        if (petData != null) {
            ListTag tamedList = petData.getTamedList();
            for (int i = 0; i < tamedList.size(); i++) {
                CompoundTag nbt = tamedList.getCompound(i);
                if (!nbt.contains("PetId")) continue;
                int petId = nbt.getInt("PetId");
                if (foundIds.contains(petId)) continue;

                String name = nbt.contains("Name") ? nbt.getString("Name") : "";
                boolean inAmulet = nbt.getBoolean("InAmulet");
                String typeDisplay = "Pet";
                String dimension = nbt.contains("Dimension") ? nbt.getString("Dimension") : "";
                int x = 0, y = 0, z = 0;
                if (nbt.contains("Pos", 9)) {
                    ListTag pos = nbt.getList("Pos", 6);
                    if (pos.size() >= 3) {
                        x = (int) Math.floor(pos.getDouble(0));
                        y = (int) Math.floor(pos.getDouble(1));
                        z = (int) Math.floor(pos.getDouble(2));
                    }
                }
                list.add(new PetEntry(name, typeDisplay, dimension, x, y, z, inAmulet));
            }
        }

        return new MoCMessageTamersNotebook(list);
    }

    public static void onMessage(MoCMessageTamersNotebook message, Supplier<NetworkEvent.Context> ctx) {
        if (DistExecutor.unsafeRunForDist(
                () -> () -> {
                    ctx.get().enqueueWork(() -> handleClient(message));
                    return true;
                },
                () -> () -> false)) {}
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(MoCMessageTamersNotebook message) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new MoCGUITamersNotebook(message.getEntries()));
    }
}
