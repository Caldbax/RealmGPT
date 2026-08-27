package com.realmgpt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OpenAIClient {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private static final Pattern OUTPUT_TEXT = Pattern.compile("\\\"type\\\"\\s*:\\s*\\\"output_text\\\".*?\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"", Pattern.DOTALL);

    static String generate(RealmGPTConfig cfg, String prompt) throws Exception {
        String instructions = "You generate Minecraft Java Edition 26.2 commands for a client-side Fabric mod used on a Realm where the player has permission to run commands. Return ONLY commands, one per line, without slash, markdown, numbering, commentary, or code fences. For command-block chains, return the commands in execution order. Prefer commands compatible with 26.2. Never include commands that grant operator status, alter permissions, ban/kick players, stop servers, or expose secrets. Request: " + prompt;
        String body = "{\"model\":\"" + esc(cfg.model) + "\",\"input\":\"" + esc(instructions) + "\"}";
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
            .timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer " + cfg.apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (res.statusCode() / 100 != 2) throw new IllegalStateException("OpenAI HTTP " + res.statusCode() + ": " + compact(res.body()));
        Matcher m = OUTPUT_TEXT.matcher(res.body());
        if (!m.find()) throw new IllegalStateException("OpenAI response did not contain output text.");
        return unescape(m.group(1));
    }

    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }
    private static String unescape(String s) { return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\"); }
    private static String compact(String s) { return s == null ? "" : s.replaceAll("\\s+", " ").substring(0, Math.min(300, s.replaceAll("\\s+", " ").length())); }
}
