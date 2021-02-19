package com.sefodopo.autobackup;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.lang3.SerializationException;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Path;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Backup {
    private final Timer timer;
    private final MinecraftServer server;
    private BackupTask task;
    private long timeLeft;
    private boolean paused;
    private int delay;
    private final Path configTime;
    private final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    private ServerCommandSource source = null;

    public Backup(MinecraftServer server) {
        this.server = server;
        timer = new Timer(true);
        delay = getDelayFromConfig();
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

    private int getDelayFromConfig() {
        return AutoBackup.getConfig().backupInterval * 60000;
    }

    public void preBackup(Object lock) {
        server.send(new ServerTask(1, () -> {
            boolean ops = AutoBackup.getConfig().broadCastBackupMessagesToOps;

            // Let everyone know that a backup has started
            server.getCommandSource().sendFeedback(new TranslatableText("com.sefodopo.autobackup.backupStarted"), ops);
            LogManager.getLogger().info("Starting Server Backup");

            // Save off
            for (ServerWorld world : server.getWorlds()) {
                if (world != null && !world.savingDisabled) {
                    world.savingDisabled = true;
                }
            }

            // Save All
            server.getPlayerManager().saveAllPlayerData();
            server.save(true, true, true);

            // Let the backupTask continue
            synchronized (lock) {
                lock.notify();
            }
        }));
    }

    public void postBackup(boolean success) {
        // Clean up after the backup
        server.send(new ServerTask(1, () -> {
            boolean ops = AutoBackup.getConfig().broadCastBackupMessagesToOps;

            // Save on
            for (ServerWorld world : server.getWorlds()) {
                if (world != null && world.savingDisabled)
                    world.savingDisabled = false;
            }

            // Send messages to keep everyone informed of the successfulness of the backup
            if (success) {
                server.getCommandSource().sendFeedback(new TranslatableText("com.sefodopo.autobackup.backedUp"), ops);
                LogManager.getLogger().info("Server Just Backed up!");
                if (this.source != null)
                    this.source.sendFeedback(new TranslatableText("com.sefodopo.autobackup.command.now.success"), false);
            }
            else {
                server.getCommandSource().sendFeedback(new TranslatableText("com.sefodopo.autobackup.backupFailed"), ops);
                LogManager.getLogger().warn("Server failed to back up, check the backup command!");
                if (this.source != null)
                    this.source.sendFeedback(new TranslatableText("com.sefodopo.autobackup.command.now.failure"), false);
            }
        }));
    }

    private boolean backup() {
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", AutoBackup.getConfig().backupCommand);
        } else {
            builder.command("sh", "-c", AutoBackup.getConfig().backupCommand);
        }
        builder.environment().put("world", server.getSaveProperties().getLevelName());
        builder.redirectErrorStream(true);
        try {
            Process p = builder.start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean now(ServerCommandSource source) {
        new BackupTask(source);
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
        if (delay != getDelayFromConfig()) {
            delay = getDelayFromConfig();
            timeLeft = -1;
            unQueBackup();
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
                LogManager.getLogger().info("Starting Backup before exiting!");
                boolean success = backup();
                LogManager.getLogger().info("Backup  " + (success ? "completed successfully" : "failed"));
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
        private final ServerCommandSource immediate;
        private final Object lock = new Object();

        private BackupTask() {
            immediate = null;
            if (paused && timeLeft > 0) {
                timer.scheduleAtFixedRate(this, timeLeft, delay);
                nextExecutionTime = this.scheduledExecutionTime() + delay;
                timeLeft = -1;
            } else {
                timer.scheduleAtFixedRate(this, delay, delay);
                nextExecutionTime = this.scheduledExecutionTime() + delay;
                server.getCommandSource().sendFeedback(
                        new TranslatableText("com.sefodopo.autobackup.scheduled", AutoBackup.getConfig().backupInterval),
                        AutoBackup.getConfig().broadCastBackupMessagesToOps);
            }
        }


        /*
         * Only call if you want the task to run one time only!
         * */
        private BackupTask(ServerCommandSource immediate) {
            this.immediate = immediate;
            if (immediate != null) {
                unQueBackup();
                source = immediate;
                task = this;
                timer.schedule(this, 0);
            }
        }

        @Override
        public void run() {
            if (scheduled) {
                preBackup(lock);
                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                postBackup(backup());
            }
            nextExecutionTime = this.scheduledExecutionTime() + getDelayFromConfig();
            if (immediate != null) {
                source = null;
                this.scheduled = false;
                queBackup();
            }
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
