package vanillaautomated.gui;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WBar;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WItemSlot;
import io.github.cottonmc.cotton.gui.widget.WPlainPanel;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandlerContext;
import vanillaautomated.VanillaAutomated;
import vanillaautomated.VanillaAutomatedBlocks;

public class CrusherBlockController extends SyncedGuiDescription {
    public CrusherBlockController(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(VanillaAutomatedBlocks.crusherBlockScreen, syncId, playerInventory, getBlockInventory(context, 3), getBlockPropertyDelegate(context, 4));

        setTitleAlignment(HorizontalAlignment.LEFT);
        WPlainPanel root = new WPlainPanel();
        root.setSize(176, 180);
        setRootPanel(root);

        WGridPanel machinePanel = new WGridPanel();
        machinePanel.setSize(9, 3);

        WItemSlot inputSlot = WItemSlot.of(blockInventory, 0);
        machinePanel.add(inputSlot, 3, 0);

        WBar fire = new WBar(VanillaAutomated.flames_background, VanillaAutomated.flames, 0, 2, WBar.Direction.UP);
        machinePanel.add(fire, 3, 1);

        WItemSlot fuelSlot = WItemSlot.of(blockInventory, 1);
        machinePanel.add(fuelSlot, 3, 2);

        WBar progress = new WBar(VanillaAutomated.progress_background, VanillaAutomated.progress, 1, 3, WBar.Direction.RIGHT);
        machinePanel.add(progress, 4, 2);

        WItemSlot outputSlot = WItemSlot.of(blockInventory, 2);
        machinePanel.add(outputSlot, 5, 2);

        root.add(machinePanel, 0, 20);

        root.add(this.createPlayerInventoryPanel(true), 7, 86);
        root.validate(this);
    }
}
