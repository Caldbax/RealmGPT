package com.realmgpt;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class RealmGPTClient implements ClientModInitializer {
    private static String pendingCommand;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            literal("gpt")
                .then(literal("status").executes(ctx -> {
                    message("RealmGPT loaded. Command execution requires your normal Realm permissions.");
                    return 1;
                }))
                .then(literal("approve").executes(ctx -> {
                    if (pendingCommand == null) {
                        message("Nothing is waiting for approval.");
                        return 0;
                    }
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.getConnection() == null) {
                        message("Not connected to a world or Realm.");
                        return 0;
                    }
                    mc.getConnection().sendCommand(pendingCommand.startsWith("/") ? pendingCommand.substring(1) : pendingCommand);
                    message("Executed: /" + pendingCommand.replaceFirst("^/", ""));
                    pendingCommand = null;
                    return 1;
                }))
                .then(literal("deny").executes(ctx -> {
                    pendingCommand = null;
                    message("Pending action discarded.");
                    return 1;
                }))
                .then(literal("stop").executes(ctx -> {
                    pendingCommand = null;
                    message("RealmGPT stopped and pending action cleared.");
                    return 1;
                }))
                .then(literal("queue")
                    .then(argument("command", StringArgumentType.greedyString()).executes(ctx -> {
                        pendingCommand = StringArgumentType.getString(ctx, "command");
                        message("Queued for approval: /" + pendingCommand.replaceFirst("^/", ""));
                        message("Run /gpt approve or /gpt deny.");
                        return 1;
                    })))
                .then(literal("chain")
                    .then(argument("description", StringArgumentType.greedyString()).executes(ctx -> {
                        String request = StringArgumentType.getString(ctx, "description");
                        message("Command-block chain request captured: " + request);
                        message("AI generation is the next build step; no command was executed.");
                        return 1;
                    })))
        ));
        message("RealmGPT initialized. Try /gpt status");
    }

    private static void message(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("[RealmGPT] " + text), false);
    }
}
