package vanillaautomated.blockentities;

import blue.endless.jankson.annotation.Nullable;
import io.github.cottonmc.cotton.gui.PropertyDelegateHolder;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import vanillaautomated.VanillaAutomated;
import vanillaautomated.VanillaAutomatedBlocks;
import vanillaautomated.gui.FisherBlockController;

import java.util.List;
import java.util.Random;

public class FisherBlockEntity extends MachineBlockEntity implements SidedInventory, PropertyDelegateHolder {

    public int speed = VanillaAutomated.config.fisherTime;
    public boolean hasWater = false;
    private Random random = new Random();

    public FisherBlockEntity(BlockPos pos, BlockState state) {
        super(VanillaAutomatedBlocks.fisherBlockEntity, pos, state);
        items = DefaultedList.ofSize(10, ItemStack.EMPTY);
    }

    public static void tick(World world, BlockPos blockPos, BlockState blockState, FisherBlockEntity t) {
        if (world.isClient) {
            return;
        }

        if (!t.hasWater) {
            t.processingTime = 0;
            return;
        }

        // Freeze when powered
        if (world.getBlockState(t.getPos()).get(Properties.POWERED).booleanValue()) {
            if (t.isBurning()) {
                t.fuelTime--;
            }

            return;
        }

        boolean changed = false;
        if (t.isBurning()) {
            t.processingTime++;
            t.fuelTime--;
        }

        ItemStack itemStack = t.items.get(0);
        if (t.canAcceptOutput()) {
            // Burn another item
            if (!t.isBurning()) {
                if (!itemStack.isEmpty()) {
                    t.maxFuelTime = t.getFuelTime(itemStack);
                    t.fuelTime = t.maxFuelTime;
                    changed = true;

                    Item item = itemStack.getItem();
                    itemStack.decrement(1);
                    if (itemStack.isEmpty()) {
                        Item item2 = item.getRecipeRemainder();
                        t.items.set(0, item2 == null ? ItemStack.EMPTY : new ItemStack(item2));
                    }
                } else {
                    t.processingTime = 0;
                }
            }

            // Generate items
            if (t.processingTime == t.speed) {
                t.processingTime = 0;
                t.generateItems();
                changed = true;
            }
        } else {
            t.processingTime = 0;
        }

        if (changed) {
            t.markDirty();
        }
    }

    public static boolean canUseAsFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.createFuelTimeMap().containsKey(stack.getItem());
    }

    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    @Override
    public int size() {
        return 10;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return (ItemStack) this.items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.items, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        ItemStack itemStack = this.items.get(slot);
        boolean bl = !stack.isEmpty() && stack.isItemEqualIgnoreDamage(itemStack) && ItemStack.areTagsEqual(stack, itemStack);
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world.getBlockEntity(this.pos) != this) {
            return false;
        } else {
            return player.squaredDistanceTo((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot > 0) {
            return false;
        } else {
            ItemStack itemStack = this.items.get(1);
            return canUseAsFuel(stack) || stack.getItem() == Items.BUCKET && itemStack.getItem() != Items.BUCKET;
        }
    }

    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        this.hasWater = tag.getBoolean("HasWater");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        tag.putBoolean("HasWater", this.hasWater);
        return super.writeNbt(tag);
    }

    private boolean canAcceptOutput() {
        for (int i = 1; i < size(); i++) {
            if (items.get(i).isEmpty() || items.get(i).getCount() < items.get(i).getMaxCount()) {
                return true;
            }
        }

        return false;
    }

    private void generateItems() {
        PlayerEntity player = world.getClosestPlayer((int) pos.getX(), (int) pos.getY(), (int) pos.getZ(), Float.MAX_VALUE, false);
        if (player == null) {
            return;
        }

        LootContext.Builder builder = (new LootContext.Builder((ServerWorld) this.world)).parameter(LootContextParameters.ORIGIN, new Vec3d(getPos().getX(), getPos().getY(), getPos().getZ())).parameter(LootContextParameters.TOOL, ItemStack.EMPTY).parameter(LootContextParameters.THIS_ENTITY, player).random(this.random);
        LootTable lootTable = this.world.getServer().getLootManager().getTable(LootTables.FISHING_GAMEPLAY);
        List<ItemStack> list = lootTable.generateLoot(builder.build(LootContextTypes.FISHING));

        for (int i = 1; i < 10; i++) {
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).isEmpty()) {
                    continue;
                }

                if (items.get(i).isEmpty()) {
                    setStack(i, list.get(j));
                    list.set(j, ItemStack.EMPTY);
                } else if (items.get(i).isItemEqual(list.get(j))) {
                    int amountToAdd = items.get(i).getMaxCount() - items.get(i).getCount();
                    amountToAdd = Math.min(amountToAdd, list.get(j).getCount());
                    items.get(i).increment(amountToAdd);
                    list.get(j).decrement(amountToAdd);
                }
            }
        }
    }

    protected int getFuelTime(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        } else {
            Item item = fuel.getItem();
            return (Integer) AbstractFurnaceBlockEntity.createFuelTimeMap().getOrDefault(item, 0);
        }
    }

    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        } else {
            return new int[]{0};
        }
    }

    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.isValid(slot, stack);
    }

    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        if (dir == Direction.DOWN && slot == 0) {
            Item item = stack.getItem();
            if (item != Items.WATER_BUCKET && item != Items.BUCKET) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void clear() {
        items = DefaultedList.ofSize(10, ItemStack.EMPTY);
    }

    public boolean isBurning() {
        return this.fuelTime > 0;
    }

    @Override
    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    @Override
    protected Text getContainerName() {
        return new TranslatableText("block." + VanillaAutomated.prefix + ".fisher_block");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new FisherBlockController(syncId, inventory, ScreenHandlerContext.create(world, pos));
    }
}
