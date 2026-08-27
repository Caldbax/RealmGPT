package com.realmgpt;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class RealmGPTClient implements ClientModInitializer {
    private static final List<String> pending = new ArrayList<>();
    private static volatile boolean busy;
    private static RealmGPTConfig config;

    @Override public void onInitializeClient() {
        reloadConfig();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
            literal("gpt")
                .then(literal("status").executes(ctx -> { feedback("Loaded | API key: " + (config.hasKey() ? "configured" : "MISSING") + " | pending: " + pending.size() + " | busy: " + busy); return 1; }))
                .then(literal("reload").executes(ctx -> { reloadConfig(); feedback("Configuration reloaded. API key: " + (config.hasKey() ? "configured" : "MISSING")); return 1; }))
                .then(literal("approve").executes(ctx -> { executePending(); return 1; }))
                .then(literal("deny").executes(ctx -> { pending.clear(); feedback("Pending commands discarded."); return 1; }))
                .then(literal("stop").executes(ctx -> { pending.clear(); busy = false; feedback("Pending work cleared."); return 1; }))
                .then(literal("queue").then(argument("command", StringArgumentType.greedyString()).executes(ctx -> { pending.clear(); pending.add(clean(StringArgumentType.getString(ctx,"command"))); feedback("Queued 1 command. Use /gpt approve."); return 1; })))
                .then(literal("ask").then(argument("request", StringArgumentType.greedyString()).executes(ctx -> { generate(StringArgumentType.getString(ctx,"request")); return 1; })))
                .then(literal("chain").then(argument("description", StringArgumentType.greedyString()).executes(ctx -> { generate("Create a command-block chain for: " + StringArgumentType.getString(ctx,"description")); return 1; })))
        ));
    }

    private static void reloadConfig() {
        try { config = RealmGPTConfig.load(FabricLoader.getInstance().getConfigDir()); }
        catch (Exception e) { config = new RealmGPTConfig("", "gpt-5-mini", true); feedback("Could not load config: " + e.getMessage()); }
    }

    private static void generate(String request) {
        if (busy) { feedback("A request is already running."); return; }
        if (!config.hasKey()) { feedback("Add your OpenAI API key to config/realmgpt/realmgpt.properties, then run /gpt reload."); return; }
        busy = true; feedback("Generating commands...");
        CompletableFuture.runAsync(() -> {
            try {
                String text = OpenAIClient.generate(config, request);
                List<String> commands = parse(text);
                Minecraft.getInstance().execute(() -> {
                    pending.clear(); pending.addAll(commands); busy = false;
                    if (commands.isEmpty()) { feedback("GPT returned no usable commands."); return; }
                    feedback("Generated " + commands.size() + " command(s). First: /" + commands.getFirst());
                    if (config.requireApproval) feedback("Review and run /gpt approve, or /gpt deny."); else executePending();
                });
            } catch (Exception e) { Minecraft.getInstance().execute(() -> { busy = false; feedback("Generation failed: " + e.getMessage()); }); }
        });
    }

    private static List<String> parse(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String c = clean(line);
            if (c.isBlank() || c.startsWith("```") || c.startsWith("#")) continue;
            String lower = c.toLowerCase();
            if (lower.startsWith("op ") || lower.startsWith("deop ") || lower.startsWith("ban ") || lower.startsWith("ban-ip ") || lower.startsWith("kick ") || lower.equals("stop") || lower.startsWith("whitelist ")) continue;
            out.add(c);
            if (out.size() >= 128) break;
        }
        return out;
    }

    private static String clean(String c) { c = c.trim(); while (c.startsWith("/")) c = c.substring(1); return c; }

    private static void executePending() {
        Minecraft mc = Minecraft.getInstance();
        if (pending.isEmpty()) { feedback("Nothing is waiting for approval."); return; }
        if (mc.getConnection() == null) { feedback("Not connected to a world/server."); return; }
        List<String> commands = new ArrayList<>(pending); pending.clear();
        feedback("Executing " + commands.size() + " command(s)...");
        runSequential(commands, 0);
    }

    private static void runSequential(List<String> commands, int index) {
        Minecraft mc = Minecraft.getInstance();
        if (index >= commands.size()) { feedback("Finished executing command set."); return; }
        if (mc.getConnection() == null) { feedback("Connection lost; stopped."); return; }
        mc.getConnection().sendCommand(commands.get(index));
        CompletableFuture.delayedExecutor(125, java.util.concurrent.TimeUnit.MILLISECONDS).execute(() -> mc.execute(() -> runSequential(commands, index + 1)));
    }

    private static void feedback(String message) {
        System.out.println("[RealmGPT] " + message);
    }
}
