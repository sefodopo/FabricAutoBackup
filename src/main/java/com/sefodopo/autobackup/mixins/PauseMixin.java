package com.sefodopo.autobackup.mixins;

import com.sefodopo.autobackup.AutoBackup;
import com.sefodopo.autobackup.Backup;
import me.sargunvohra.mcmods.autoconfig1u.shadowed.blue.endless.jankson.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class PauseMixin {

    @Shadow
    private boolean paused;

    @Shadow
    public abstract boolean isIntegratedServerRunning();

    @Shadow
    @Nullable
    public Screen currentScreen;

    @Shadow
    @Nullable
    public Overlay overlay;

    @Shadow
    @Nullable
    private IntegratedServer server;

    @Inject(method = "render(Z)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/MinecraftClient;isIntegratedServerRunning()Z"))
    private void afterChange(CallbackInfo info) {
        boolean bl = this.isIntegratedServerRunning() && (this.currentScreen != null && this.currentScreen.isPauseScreen() || this.overlay != null && this.overlay.pausesGame()) && !this.server.isRemote();
        if (this.paused != bl) {
            AutoBackup mod = AutoBackup.getInstance();
            if (mod != null) {
                Backup backup = mod.getBackup();
                if (backup != null)
                    backup.setPauseStatus(bl);
            }
        }
    }

}
