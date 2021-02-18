package com.sefodopo.autobackup;

import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry;
import me.sargunvohra.mcmods.autoconfig1u.serializer.Toml4jConfigSerializer;
import me.sargunvohra.mcmods.autoconfig1u.shadowed.blue.endless.jankson.Comment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class AutoBackup implements ModInitializer {

    private static ModConfig config;
    private Backup backup;
    private static AutoBackup mod;

    @Override
    public void onInitialize() {
        getConfig();
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> new BackupCommands(dispatcher, this));
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> backup = new Backup(server));
        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            backup.stop();
            backup = null;
        });
        mod = this;
    }

    public static AutoBackup getInstance() {
        return mod;
    }

    public static ModConfig getConfig() {
        if (config != null) {
            return config;
        }
        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        return config;
    }

    public Backup getBackup() {
        return backup;
    }

    @Config(name = "autobackup")
    public static class ModConfig implements ConfigData {
        boolean enableBackup = false;
        boolean autoBackup = true;
        int backupInterval = 60;
        String backupCommand = "";
        @Comment("Runs a backup when you save and quit a world")
        boolean backupWhenExit = false;
        @Comment("Does not apply when Backup When Exit is true")
        boolean saveTimeLeft = true;
        boolean broadCastBackupMessagesToOps = true;
        @ConfigEntry.Category("permissions") int statusPermissionLevel = 4;
        @ConfigEntry.Category("permissions") int permissionLevel = 4;
        @ConfigEntry.Category("permissions") int backupNowPermissionLevel = 4;
        @ConfigEntry.Category("permissions") int commandPermissionLevel = 5;
    }


}
