package com.sefodopo.autobackup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class BackupCommands {
    private final AutoBackup mod;

    public BackupCommands(CommandDispatcher<ServerCommandSource> dispatcher, AutoBackup mod) {
        this.mod = mod;

        dispatcher.register(CommandManager.literal("backup")
                .requires(source -> source.hasPermissionLevel(AutoBackup.getConfig().statusPermissionLevel))
                .then(status())
                .then(minutes())
                .then(enable())
                .then(disable())
                .then(command())
                .then(now())
                .then(master())
                .executes(context -> {
                    sendStatus(context.getSource());
                    return 0;
                }));
    }

    private void sendStatus(ServerCommandSource source) {
        source.sendFeedback(AutoBackup.text("com.sefodopo.autobackup.command.status",
                AutoBackup.getConfig().enableBackup ? "enabled" : "disabled",
                AutoBackup.getConfig().autoBackup ? "enabled" : "disabled",
                AutoBackup.getConfig().backupInterval,
                mod.getBackup().getMinutesUntilBackup()), false);
    }

    private LiteralArgumentBuilder<ServerCommandSource> status() {
        return CommandManager.literal("status")
                .executes(cxt -> {
                    sendStatus(cxt.getSource());
                    return 0;
                });
    }

    private LiteralArgumentBuilder<ServerCommandSource> minutes() {
        return CommandManager.literal("minutes")
                .requires(source -> source.hasPermissionLevel(AutoBackup.getConfig().permissionLevel))
                .then(CommandManager.argument("minutes", IntegerArgumentType.integer(0))
                        .executes(cxt -> {
                            AutoBackup.getConfig().backupInterval = IntegerArgumentType.getInteger(cxt, "minutes");
                            AutoBackup.saveConfig();
                            sendStatus(cxt.getSource());
                            return 0;
                        }))
                .executes(cxt -> {
                    cxt.getSource().sendFeedback(AutoBackup.text("com.sefodopo.autobackup.command.minutes", AutoBackup.getConfig().backupInterval), false);
                    return 0;
                });
    }

    private LiteralArgumentBuilder<ServerCommandSource> enable() {
        return CommandManager.literal("enable")
                .requires(source -> source.hasPermissionLevel(AutoBackup.getConfig().permissionLevel))
                .executes(cxt -> {
                    if (AutoBackup.getConfig().autoBackup) {
                        cxt.getSource().sendError(AutoBackup.text("com.sefodopo.autobackup.command.enable.failure"));
                        return 1;
                    }
                    AutoBackup.getConfig().autoBackup = true;
                    AutoBackup.saveConfig();
                    sendStatus(cxt.getSource());
                    return 0;
                });
    }

    private LiteralArgumentBuilder<ServerCommandSource> disable() {
        return CommandManager.literal("disable")
                .requires(source -> source.hasPermissionLevel(AutoBackup.getConfig().permissionLevel))
                .executes(cxt -> {
                    if (!AutoBackup.getConfig().autoBackup) {
                        cxt.getSource().sendError(AutoBackup.text("com.sefodopo.autobackup.command.disable.failure"));
                        return 1;
                    }
                    AutoBackup.getConfig().autoBackup = false;
                    AutoBackup.saveConfig();
                    sendStatus(cxt.getSource());
                    return 0;
                });
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("command")
                .requires(source -> source.hasPermissionLevel(AutoBackup.getConfig().commandPermissionLevel))
                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                        .executes(cxt -> {
                            AutoBackup.getConfig().backupCommand = StringArgumentType.getString(cxt, "command");
                            AutoBackup.saveConfig();
                            sendStatus(cxt.getSource());
                            return 0;
                        }))
                .executes(cxt -> {
                    cxt.getSource().sendFeedback(AutoBackup.text("com.sefodopo.autobackup.command.command", AutoBackup.getConfig().backupCommand), false);
                    return 0;
                });
    }

    private LiteralArgumentBuilder<ServerCommandSource> now() {
        return CommandManager.literal("now")
                .requires(source -> source.hasPermissionLevel(AutoBackup.getConfig().backupNowPermissionLevel))
                .executes(cxt -> {
                    if (!AutoBackup.getConfig().enableBackup) {
                        cxt.getSource().sendError(AutoBackup.text("com.sefodopo.autobackup.command.now.disabled"));
                        return 1;
                    }
                    if (AutoBackup.getInstance().getBackup().now(cxt.getSource())) {
                        cxt.getSource().sendFeedback(AutoBackup.text("com.sefodopo.autobackup.command.now.started"), false);
                        return 0;
                    }
                    cxt.getSource().sendError(AutoBackup.text("com.sefodopo.autobackup.command.now.failure"));
                    return 1;
                });
    }

    private LiteralArgumentBuilder<ServerCommandSource> master() {
        return CommandManager.literal("master")
                .requires(source -> source.hasPermissionLevel(AutoBackup.getConfig().permissionLevel))
                .then(CommandManager.literal("enable")
                        .executes(cxt -> {
                            if (AutoBackup.getConfig().enableBackup) {
                                cxt.getSource().sendError(AutoBackup.text("com.sefodopo.autobackup.command.master.enableFailure"));
                                return 1;
                            }
                            AutoBackup.getConfig().enableBackup = true;
                            AutoBackup.saveConfig();
                            sendStatus(cxt.getSource());
                            return 0;
                        }))
                .then(CommandManager.literal("disable")
                        .executes(cxt -> {
                            if (!AutoBackup.getConfig().enableBackup) {
                                cxt.getSource().sendError(AutoBackup.text("com.sefodopo.autobackup.command.master.disableFailure"));
                                return 1;
                            }
                            AutoBackup.getConfig().enableBackup = false;
                            AutoBackup.saveConfig();
                            sendStatus(cxt.getSource());
                            return 0;
                        }));
    }

}
