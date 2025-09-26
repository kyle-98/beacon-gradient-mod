package com.kronichiwa;

import com.kronichiwa.utils.ColorUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class BeaconGradient implements ClientModInitializer {
	public static final String MOD_ID = "beacon-gradient";

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommand(dispatcher));
	}

    
	private void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher)  {
		dispatcher.register(
                ClientCommandManager.literal("beacon_gradient")
                    .then(ClientCommandManager.argument("startColor", StringArgumentType.string())
                        .then(ClientCommandManager.argument("endColor", StringArgumentType.string())
                            .then(ClientCommandManager.argument("beacons", IntegerArgumentType.integer(1))
                                .then(ClientCommandManager.argument("maxStack", IntegerArgumentType.integer(1, 7))
                                    .then(ClientCommandManager.argument("beamWidth", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            String start = StringArgumentType.getString(ctx, "startColor");
                                            String end = StringArgumentType.getString(ctx, "endColor");
                                            int beacons = IntegerArgumentType.getInteger(ctx, "beacons");
                                            int maxStack = IntegerArgumentType.getInteger(ctx, "maxStack");
                                            int beamWidth = IntegerArgumentType.getInteger(ctx, "beamWidth");
                                            runGradientAsync(start, end, beacons, maxStack, beamWidth);
                                            return 1;
                                        }))
                                    // executes with maxStack but no beamWidth -> default beamWidth
                                    .executes(ctx -> {
                                        String start = StringArgumentType.getString(ctx, "startColor");
                                        String end = StringArgumentType.getString(ctx, "endColor");
                                        int beacons = IntegerArgumentType.getInteger(ctx, "beacons");
                                        int maxStack = IntegerArgumentType.getInteger(ctx, "maxStack");
                                        int beamWidth = 1200; // default
                                        runGradientAsync(start, end, beacons, maxStack, beamWidth);
                                        return 1;
                                    }))
                                // executes with beacons but no maxStack -> default maxStack & beamWidth
                                .executes(ctx -> {
                                    String start = StringArgumentType.getString(ctx, "startColor");
                                    String end = StringArgumentType.getString(ctx, "endColor");
                                    int beacons = IntegerArgumentType.getInteger(ctx, "beacons");
                                    int maxStack = 7; // default
                                    int beamWidth = 1200; // default
                                    runGradientAsync(start, end, beacons, maxStack, beamWidth);
                                    return 1;
                                }))
                            ))
                    // show required args for command
                    .executes(ctx -> {
                        sendToPlayer(Text.literal("Usage: /beacon_gradient <startColor> <endColor> <beacons> [maxStack] [beamWidth]").setStyle(Style.EMPTY));
                        return 1;
                    })
            );
	}


	private void runGradientAsync(String startInput, String endInput, int beacons, int maxStack, int beamWidth) {
        new Thread(() -> {
            try {
                ColorUtils.runAndSendToPlayer(startInput, endInput, beacons, maxStack, beamWidth);
            } catch (Throwable t) {
                sendToPlayer(Text.literal("beacon_gradient: error: " + t.getMessage()));
                t.printStackTrace();
            }
        }, "BeaconGradient-Worker").start();
    }


    public static void sendToPlayer(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(text, false);
        }
    }


    public static MutableText colorSwatch(float[] rgb) {
        int r = Math.max(0, Math.min(255, Math.round(rgb[0] * 255f)));
        int g = Math.max(0, Math.min(255, Math.round(rgb[1] * 255f)));
        int b = Math.max(0, Math.min(255, Math.round(rgb[2] * 255f)));
        int color = (r << 16) | (g << 8) | b;
        String blocks = "███";
        return Text.literal(blocks).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }
}