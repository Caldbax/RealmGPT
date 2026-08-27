package com.realmgpt;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class RealmGPTClient implements ClientModInitializer {
    private static String pendingCommand;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
            literal("gpt")
                .then(literal("status").executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("[RealmGPT] RealmGPT loaded."));
                    return 1;
                }))
                .then(literal("approve").executes(ctx -> {
                    if (pendingCommand == null) {
                        ctx.getSource().sendFeedback(Component.literal("[RealmGPT] Nothing is waiting for approval."));
                        return 0;
                    }
                    ctx.getSource().sendFeedback(Component.literal("[RealmGPT] Approved: /" + pendingCommand.replaceFirst("^/", "")));
                    pendingCommand = null;
                    return 1;
                }))
                .then(literal("deny").executes(ctx -> {
                    pendingCommand = null;
                    ctx.getSource().sendFeedback(Component.literal("[RealmGPT] Pending action discarded."));
                    return 1;
                }))
                .then(literal("stop").executes(ctx -> {
                    pendingCommand = null;
                    ctx.getSource().sendFeedback(Component.literal("[RealmGPT] RealmGPT stopped and pending action cleared."));
                    return 1;
                }))
                .then(literal("queue")
                    .then(argument("command", StringArgumentType.greedyString()).executes(ctx -> {
                        pendingCommand = StringArgumentType.getString(ctx, "command");
                        ctx.getSource().sendFeedback(Component.literal("[RealmGPT] Queued for approval: /" + pendingCommand.replaceFirst("^/", "")));
                        return 1;
                    })))
                .then(literal("chain")
                    .then(argument("description", StringArgumentType.greedyString()).executes(ctx -> {
                        String request = StringArgumentType.getString(ctx, "description");
                        ctx.getSource().sendFeedback(Component.literal("[RealmGPT] Command-block chain request captured: " + request));
                        return 1;
                    })))
        ));
    }
}
