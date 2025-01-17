package vanillaautomated.gui;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WPlainPanel;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import vanillaautomated.VanillaAutomated;
import vanillaautomated.VanillaAutomatedBlocks;

public class TimerController extends SyncedGuiDescription {
    private int time;
    private WLabel speedLabel;

    public TimerController(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, BlockPos blockPos, int currentTime) {
        super(VanillaAutomatedBlocks.timerBlockScreen, syncId, playerInventory, getBlockInventory(context, 0), getBlockPropertyDelegate(context, 0));

        time = currentTime;

        setTitleAlignment(HorizontalAlignment.LEFT);
        WPlainPanel root = new WPlainPanel();
        root.setSize(176, 170);
        setRootPanel(root);

        int rowHeight = 40;

        WLabel speedTitle = new WLabel(new TranslatableText("vanillaautomated.container.timer.speed"));
        speedTitle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        root.add(speedTitle, 0, 30, 160, 10);

        speedLabel = new WLabel(time + "");
        speedLabel.setHorizontalAlignment(HorizontalAlignment.CENTER);
        speedLabel.setSize(160, 30);
        root.add(speedLabel, 72, rowHeight + 5);

        WButton speedButton = new WButton(new LiteralText("-10"));
        speedButton.setOnClick(new Runnable() {
            @Override
            public void run() {
                sendPacket(-10, blockPos);
            }
        });
        root.add(speedButton, 18, rowHeight, 27, 18);

        speedButton = new WButton(new LiteralText("-1"));
        speedButton.setOnClick(new Runnable() {
            @Override
            public void run() {
                sendPacket(-1, blockPos);
            }
        });
        root.add(speedButton, 45, rowHeight, 27, 18);

        speedButton = new WButton(new LiteralText("+1"));
        speedButton.setOnClick(new Runnable() {
            @Override
            public void run() {
                sendPacket(1, blockPos);
            }
        });
        root.add(speedButton, 90, rowHeight, 27, 18);

        speedButton = new WButton(new LiteralText("+10"));
        speedButton.setOnClick(new Runnable() {
            @Override
            public void run() {
                sendPacket(10, blockPos);
            }
        });
        root.add(speedButton, 117, rowHeight, 27, 18);

        root.add(this.createPlayerInventoryPanel(true), 7, 76);
        root.validate(this);
    }

    private void sendPacket (int change, BlockPos blockPos) {
        time += change;
        time = Math.max(2, time); // 1/10 second
        time = Math.min(72000, time); // 1 hour

        speedLabel.setText(new LiteralText(time + ""));

        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
        passedData.writeBlockPos(blockPos);
        passedData.writeInt(change);
        // Send packet to server to change the block for us
        ClientSidePacketRegistry.INSTANCE.sendToServer(VanillaAutomated.timer_configuration_packet, passedData);
    }
}
