package com.realmgpt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class RealmGPTConfig {
    final String apiKey;
    final String model;
    final boolean requireApproval;

    RealmGPTConfig(String apiKey, String model, boolean requireApproval) {
        this.apiKey = apiKey;
        this.model = model;
        this.requireApproval = requireApproval;
    }

    static RealmGPTConfig load(Path configDir) throws IOException {
        Path dir = configDir.resolve("realmgpt");
        Files.createDirectories(dir);
        Path file = dir.resolve("realmgpt.properties");
        if (!Files.exists(file)) Files.writeString(file, "# RealmGPT local configuration - DO NOT SHARE THIS FILE\napi_key=PASTE_OPENAI_API_KEY_HERE\nmodel=gpt-5-mini\nrequire_approval=true\n");
        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) { p.load(in); }
        return new RealmGPTConfig(p.getProperty("api_key", "").trim(), p.getProperty("model", "gpt-5-mini").trim(), Boolean.parseBoolean(p.getProperty("require_approval", "true")));
    }

    boolean hasKey() { return !apiKey.isBlank() && !apiKey.equals("PASTE_OPENAI_API_KEY_HERE"); }
}
