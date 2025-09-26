package com.kronichiwa.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class GradientLogic {
    public static void runGradient(String startInput, String endInput,
                                   int beacons, int maxStack, int beamWidth,
                                   PlayerEntity player) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client != null) {
            client.execute(() -> {
                if (player != null) {
                    player.sendMessage(Text.literal("beacon_gradient: computing gradient..."), false);
                }
            });
        }

        new Thread(() -> {
            try {
                ColorUtils.runAndSendToPlayer(startInput, endInput, beacons, maxStack, beamWidth);
            } catch (Throwable t) {
                if (client != null) {
                    client.execute(() -> {
                        if (player != null) {
                            player.sendMessage(Text.literal("beacon_gradient: error: " + t.getMessage()), false);
                        }
                    });
                }
                t.printStackTrace();
            }
        }, "BeaconGradient-Worker").start();
    }
}
