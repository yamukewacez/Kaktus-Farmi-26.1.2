package com.example.client;

import com.mojang.blaze3d.platform.InputConstants;
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

public class CactusBuilderClient implements ClientModInitializer {

    // GLFW Tuş Kodları
    private static final int KEY_A = 65;
    private static final int KEY_Z = 90;
    private static final int KEY_X = 88;
    private static final int KEY_C = 67;
    private static final int KEY_LEFT_ALT = 342;
    private static final int KEY_RIGHT_ALT = 346;

    // Ana Açma/Kapatma Durumu
    private static boolean isEnabled = false;

    // Otomatik Sürekli Koyma Durumları
    private static boolean autoPlaceString = false;
    private static boolean autoPlaceSand = false;
    private static boolean autoPlaceCactus = false;

    // Tuş basım tetikleyicileri (tek basış yakalamak için)
    private static boolean wasADown = false;
    private static boolean wasZDown = false;
    private static boolean wasXDown = false;
    private static boolean wasCDown = false;

    // Koyma sıklığı gecikmesi (tick bazlı spam önleme)
    private static int placeCooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }

            boolean isAltPressed = InputConstants.isKeyDown(KEY_LEFT_ALT) || InputConstants.isKeyDown(KEY_RIGHT_ALT);
            boolean isADown = InputConstants.isKeyDown(KEY_A);
            boolean isZDown = InputConstants.isKeyDown(KEY_Z);
            boolean isXDown = InputConstants.isKeyDown(KEY_X);
            boolean isCDown = InputConstants.isKeyDown(KEY_C);

            // Alt + A: Modu Başlat / Durdur
            if (isAltPressed && isADown && !wasADown) {
                isEnabled = !isEnabled;
                if (!isEnabled) {
                    autoPlaceString = false;
                    autoPlaceSand = false;
                    autoPlaceCactus = false;
                    sendMessage(client, "§c[CactusBuilder] Mod DURDURULDU!");
                } else {
                    sendMessage(client, "§a[CactusBuilder] Mod BAŞLATILDI! (Tuşları aktif edebilirsin)");
                }
            }

            // Mod kapalıysa diğer fonksiyonlar çalışmaz
            if (isEnabled && isAltPressed) {
                // Alt + Z: Sürekli İp Aç/Kapat
                if (isZDown && !wasZDown) {
                    autoPlaceString = !autoPlaceString;
                    sendMessage(client, autoPlaceString ? "§e[+] Sürekli İP Modu: AKTİF" : "§7[-] Sürekli İP Modu: KAPALI");
                }
                // Alt + X: Sürekli Kum Aç/Kapat
                if (isXDown && !wasXDown) {
                    autoPlaceSand = !autoPlaceSand;
                    sendMessage(client, autoPlaceSand ? "§e[+] Sürekli KUM Modu: AKTİF" : "§7[-] Sürekli KUM Modu: KAPALI");
                }
                // Alt + C: Sürekli Kaktüs Aç/Kapat
                if (isCDown && !wasCDown) {
                    autoPlaceCactus = !autoPlaceCactus;
                    sendMessage(client, autoPlaceCactus ? "§e[+] Sürekli KAKTÜS Modu: AKTİF" : "§7[-] Sürekli KAKTÜS Modu: KAPALI");
                }
            }

            wasADown = isADown;
            wasZDown = isZDown;
            wasXDown = isXDown;
            wasCDown = isCDown;

            // Sürekli Koyma Döngüsü
            if (isEnabled) {
                if (placeCooldown > 0) {
                    placeCooldown--;
                    return;
                }

                if (autoPlaceString) {
                    tryPlace(client, TargetType.STRING);
                }
                if (autoPlaceSand) {
                    tryPlace(client, TargetType.SAND);
                }
                if (autoPlaceCactus) {
                    tryPlace(client, TargetType.CACTUS);
                }
            }
        });
    }

    private enum TargetType {
        STRING, SAND, CACTUS
    }

    private void tryPlace(Minecraft client, TargetType type) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BlockPos targetPos = hit.getBlockPos();
        Direction side = hit.getDirection();
        Block targetedBlock = client.level.getBlockState(targetPos).getBlock();
        BlockPos placePos = targetPos.relative(side);

        if (!client.level.getBlockState(placePos).isAir()) {
            return;
        }

        Item itemToUse;

        switch (type) {
            case STRING:
                // Sadece kaktüsün yatay kenarına ip koy
                if (targetedBlock != Blocks.CACTUS || !side.getAxis().isHorizontal()) {
                    return;
                }
                itemToUse = Items.STRING;
                break;

            case SAND:
                // İpin / tripwire'ın sadece ÜSTÜNE kum koy
                if (targetedBlock != Blocks.TRIPWIRE_HOOK && targetedBlock != Blocks.TRIPWIRE) {
                    return;
                }
                if (side != Direction.UP) {
                    return;
                }
                itemToUse = Items.SAND;
                break;

            case CACTUS:
                // Sadece kumun ÜSTÜNE kaktüs koy
                if ((targetedBlock != Blocks.SAND && targetedBlock != Blocks.RED_SAND) || side != Direction.UP) {
                    return;
                }
                itemToUse = Items.CACTUS;
                break;

            default:
                return;
        }

        executePlacement(client, hit, itemToUse);
    }

    private void executePlacement(Minecraft client, BlockHitResult hit, Item item) {
        Inventory inv = client.player.getInventory();
        int slot = -1;

        // Hotbar'da eşyayı ara (0-8 slotlar)
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) {
                slot = i;
                break;
            }
        }

        if (slot == -1) return;

        int previousSlot = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        inv.setSelectedSlot(previousSlot);

        // Sunucunun kick atmaması ve yerleşimin stabil olması için 2 tick cooldown
        placeCooldown = 2;
    }

    private void sendMessage(Minecraft client, String text) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(text), true);
        }
    }
}