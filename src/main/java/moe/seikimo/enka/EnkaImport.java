package moe.seikimo.enka;

import emu.grasscutter.plugin.Plugin;
import lombok.Getter;
import okhttp3.OkHttpClient;

public final class EnkaImport extends Plugin {
    @Getter private static final OkHttpClient httpClient
            = new OkHttpClient();

    @Getter private static EnkaImport instance;

    @Override
    public void onLoad() {
        EnkaImport.instance = this;

        this.getLogger().info("Loaded EnkaImport.");
    }

    @Override
    public void onEnable() {
        // Register commands.
        this.getHandle().registerCommand(new ImportCommand());

        this.getLogger().info("Enabled EnkaImport.");
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Disabled EnkaImport.");
    }
}
