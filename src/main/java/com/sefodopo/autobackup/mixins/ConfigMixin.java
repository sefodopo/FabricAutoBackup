package com.sefodopo.autobackup.mixins;

import com.sefodopo.autobackup.AutoBackup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigManager;

@Mixin(value = ConfigManager.class, remap = false)
public abstract class ConfigMixin<T extends ConfigData> {

    @Shadow
    private T config;

    @Inject(method = "save", at = @At("TAIL"), remap = false)
    public void save(CallbackInfo info) {
        if (config != null && config instanceof AutoBackup.ModConfig) {
            if (AutoBackup.getInstance() != null && AutoBackup.getInstance().getBackup() != null)
                AutoBackup.getInstance().getBackup().checkConfig();
        }
    }
}
