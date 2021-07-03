package com.sefodopo.autobackup;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigManager;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.logandark.languagehack.SSTranslatableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class AutoBackup implements ClientModInitializer, DedicatedServerModInitializer {

    private static ModConfig config;
    private Backup backup;
    private static AutoBackup mod;
    private static boolean dedicated;
    private static ConfigManager<ModConfig> configManager;


    public void onInitialize() {
        getConfig();
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> new BackupCommands(dispatcher, this));
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            backup = new Backup(server);
            if (dedicated)
                backup.queBackup();
        });
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
        configManager = (ConfigManager<ModConfig>) AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        config = configManager.getConfig();
        return config;
    }

    public Backup getBackup() {
        return backup;
    }

    @Override
    public void onInitializeClient() {
        dedicated = false;
        onInitialize();
    }

    @Override
    public void onInitializeServer() {
        dedicated = true;
        onInitialize();
    }

    public static Text text(String key) {
        if(dedicated)
            return  new SSTranslatableText(key);
        else
            return new TranslatableText(key);
    }

    public static Text text(String key, Object... args) {
        if (dedicated)
            return  new SSTranslatableText(key, args);
        else
            return new TranslatableText(key, args);
    }

    public static void saveConfig() {
        configManager.save();
        getInstance().backup.checkConfig();
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
