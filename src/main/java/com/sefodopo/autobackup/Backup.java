package com.sefodopo.autobackup;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.lang3.SerializationException;

import java.io.*;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class Backup {
    private Timer timer;
    private final MinecraftServer server;
    private BackupTask task;
    private long timeLeft = -1;
    private boolean paused = false;
    private int delay;
    private final Path configTime;
    private final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public Backup(MinecraftServer server) {
        this.server = server;
        timer = new Timer(true);
        delay = AutoBackup.getConfig().backupInterval;
        paused = true;
        configTime = server.getSavePath(WorldSavePath.ROOT).resolve("autobackup.timeleft");
        try {
            FileInputStream fis = new FileInputStream(configTime.toFile());
            ObjectInputStream ois = new ObjectInputStream(fis);
            timeLeft = ois.readLong();
            ois.close();
            fis.close();
        } catch (FileNotFoundException e) {
            timeLeft = -1;
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    public void backup() {
        server.send(new ServerTask(20, () -> {
            if (!AutoBackup.getConfig().enableBackup) {
                return;
            }
            // Save off
            Iterator<ServerWorld> worlds = server.getWorlds().iterator();
            while (worlds.hasNext()) {
                ServerWorld serverWorld = (ServerWorld) worlds.next();
                if (serverWorld != null && !serverWorld.savingDisabled) {
                    serverWorld.savingDisabled = true;
                }
            }

            // Save All
            server.getPlayerManager().saveAllPlayerData();
            server.save(true, true, true);

            // backup!!!!
            boolean success = backupWithoutSave();

            // Save On
            worlds = server.getWorlds().iterator();
            while (worlds.hasNext()) {
                ServerWorld serverWorld = (ServerWorld) worlds.next();
                if (serverWorld != null && serverWorld.savingDisabled) {
                    serverWorld.savingDisabled = false;
                }
            }
            if (success)
                server.getCommandSource().sendFeedback(new TranslatableText("com.sefodopo.autobackup.backedUp"), true);
            else
                server.getCommandSource().sendFeedback(new TranslatableText("com.sefodopo.autobackup.backupFailed"), true);
        }));

    }

    private boolean backupWithoutSave() {
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", AutoBackup.getConfig().backupCommand);
        } else {
            builder.command("sh", "-c", AutoBackup.getConfig().backupCommand);
        }
        builder.environment().put("world", server.getSaveProperties().getLevelName());
        builder.redirectErrorStream(true);
        boolean success = true;
        try {
            Process p = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    public boolean now() {
        unQueBackup();
        server.send(new ServerTask(20, this::backup));
        queBackup();
        return true;
    }

    public void queBackup() {
        if (AutoBackup.getConfig().enableBackup && AutoBackup.getConfig().autoBackup && !(task != null && task.scheduled)) {
            task = new BackupTask();
        }
    }

    public void checkConfig() {
        if (!(AutoBackup.getConfig().enableBackup && AutoBackup.getConfig().autoBackup)) {
            if (this.timeLeft > 0)
                server.getCommandSource().sendFeedback(
                        new TranslatableText("com.sefodopo.autobackup.canceled"), true);
            this.timeLeft = -1;
            unQueBackup();
        }
        if (delay != AutoBackup.getConfig().backupInterval) {
            delay = AutoBackup.getConfig().backupInterval;
            timeLeft = -1;
        }
        queBackup();
    }

    public void stop() {
        if (AutoBackup.getConfig().enableBackup) {
            if (AutoBackup.getConfig().saveTimeLeft && task != null && task.scheduled) {
                timeLeft = task.nextExecutionTime - new Date().getTime();
            } else {
                timeLeft = -1;
            }
            if (AutoBackup.getConfig().backupWhenExit) {
                backupWithoutSave();
                timeLeft = -1;
            }
        }
        timer.cancel();

        //save timeLefts
        try {
            FileOutputStream fos = new FileOutputStream(configTime.toFile());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeLong(timeLeft);
            oos.close();
            fos.close();
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    public void unQueBackup() {
        if (task != null && task.scheduled) {
            task.cancel();
        }
    }

    public void setPauseStatus(boolean paused) {
        if (paused) {
            this.paused = true;
            if (task != null && task.scheduled) {
                timeLeft = task.nextExecutionTime - new Date().getTime();
                unQueBackup();
            }
        } else {
            queBackup();
            this.paused = false;
        }
    }

    public int getMinutesUntilBackup() {
        if (task != null && task.scheduled) {
            return (int) (Math.floorDiv(task.nextExecutionTime - new Date().getTime(), 60000)) + 1;
        }
        return -1;
    }

    private class BackupTask extends TimerTask {
        private boolean scheduled = true;
        private long nextExecutionTime;

        private BackupTask() {
            int delay = AutoBackup.getConfig().backupInterval * 60000;
            if (paused && timeLeft > 0) {
                timer.scheduleAtFixedRate(this, timeLeft, delay);
                nextExecutionTime = this.scheduledExecutionTime() + delay;
                timeLeft = -1;
            } else {
                timer.scheduleAtFixedRate(this, delay, delay);
                nextExecutionTime = this.scheduledExecutionTime() + delay;
                server.getCommandSource().sendFeedback(
                        new TranslatableText("com.sefodopo.autobackup.scheduled", AutoBackup.getConfig().backupInterval),
                        true);
            }
        }

        @Override
        public void run() {
            if (scheduled) {
                backup();
            }
            nextExecutionTime = this.scheduledExecutionTime() + 60000 * AutoBackup.getConfig().backupInterval;
        }

        @Override
        public boolean cancel() {
            if (scheduled) {
                scheduled = false;
            }
            return super.cancel();
        }
    }
}
