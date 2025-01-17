package vanillaautomated.blocks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import vanillaautomated.VanillaAutomated;
import vanillaautomated.VanillaAutomatedBlocks;
import vanillaautomated.blockentities.CobblestoneGeneratorBlockEntity;

import java.util.Random;

public class CobblestoneGeneratorBlock extends MachineBlock {

    public CobblestoneGeneratorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CobblestoneGeneratorBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof CobblestoneGeneratorBlockEntity)) {
            return;
        }

        if (itemStack.hasCustomName()) {
            ((CobblestoneGeneratorBlockEntity) blockEntity).setCustomName(itemStack.getName());
        }

        ((CobblestoneGeneratorBlockEntity) blockEntity).speed = VanillaAutomated.config.cobblestoneGeneratorTime;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean notify) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof CobblestoneGeneratorBlockEntity) {
                ItemScatterer.spawn(world, pos, (Inventory) ((CobblestoneGeneratorBlockEntity) blockEntity));
                world.updateComparators(pos, this);
            }
        }

        super.onStateReplaced(state, world, pos, newState, notify);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        BlockEntity be = world.getBlockEntity(pos);
        if (be != null && be instanceof CobblestoneGeneratorBlockEntity) {
            player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
            player.incrementStat(VanillaAutomatedBlocks.interactWithCobblestoneGenerator);
        }

        return ActionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (((CobblestoneGeneratorBlockEntity) world.getBlockEntity(pos)).isBurning()) {
            super.particles(state, world, pos, random);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, VanillaAutomatedBlocks.cobblestoneGeneratorBlockEntity, CobblestoneGeneratorBlockEntity::tick);
    }
}
