package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class CactusBuilderClient implements ClientModInitializer {

    private static boolean enabled = false;
    private static boolean autoString = false;
    private static boolean autoSand = false;
    private static boolean autoCactus = false;

    private static boolean wasA = false;
    private static boolean wasZ = false;
    private static boolean wasX = false;
    private static boolean wasC = false;

    private static int cooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            long window = client.getWindow().getWindow();

            boolean alt = key(window, GLFW.GLFW_KEY_LEFT_ALT)
                       || key(window, GLFW.GLFW_KEY_RIGHT_ALT);

            boolean a = alt && key(window, GLFW.GLFW_KEY_A);
            boolean z = alt && key(window, GLFW.GLFW_KEY_Z);
            boolean x = alt && key(window, GLFW.GLFW_KEY_X);
            boolean c = alt && key(window, GLFW.GLFW_KEY_C);

            if (client.screen != null) {
                wasA = a;
                wasZ = z;
                wasX = x;
                wasC = c;
                return;
            }

            if (a && !wasA) {
                enabled = !enabled;
                if (!enabled) {
                    autoString = false;
                    autoSand = false;
                    autoCactus = false;
                }
                message(client, enabled
                    ? "§aKaktus Farmi BASLATILDI"
                    : "§cKaktus Farmi DURDURULDU");
            }

            if (enabled) {
                if (z && !wasZ) {
                    autoString = !autoString;
                    message(client, "Ip: " + (autoString ? "ACIK" : "KAPALI"));
                }

                if (x && !wasX) {
                    autoCactus = !autoCactus;
                    message(client, "Kaktus: " + (autoCactus ? "ACIK" : "KAPALI"));
                }

                if (c && !wasC) {
                    autoSand = !autoSand;
                    message(client, "Kum: " + (autoSand ? "ACIK" : "KAPALI"));
                }
            }

            wasA = a;
            wasZ = z;
            wasX = x;
            wasC = c;

            if (!enabled) return;
            if (cooldown > 0) {
                cooldown--;
                return;
            }

            if (autoString) place(client, 0);
            if (autoSand) place(client, 1);
            if (autoCactus) place(client, 2);
        });
    }

    private static boolean key(long window, int code) {
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }

    private static void place(Minecraft client, int mode) {
        if (!(client.hitResult instanceof BlockHitResult hit)) return;
        if (client.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getDirection();
        Block block = client.level.getBlockState(pos).getBlock();

        Item item;

        if (mode == 0) {
            if (block != Blocks.CACTUS || !side.getAxis().isHorizontal()) return;
            item = Items.STRING;
        } else if (mode == 1) {
            if (block != Blocks.TRIPWIRE || side != Direction.UP) return;
            item = Items.SAND;
        } else {
            if ((block != Blocks.SAND && block != Blocks.RED_SAND)
                || side != Direction.UP) return;
            item = Items.CACTUS;
        }

        Inventory inv = client.player.getInventory();
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) {
                slot = i;
                break;
            }
        }

        if (slot == -1) return;

        int previous = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);

        try {
            client.gameMode.useItemOn(
                client.player,
                InteractionHand.MAIN_HAND,
                hit
            );
            cooldown = 3;
        } finally {
            inv.setSelectedSlot(previous);
        }
    }

    private static void message(Minecraft client, String text) {
        client.player.displayClientMessage(
            Component.literal(text), true
        );
    }
}