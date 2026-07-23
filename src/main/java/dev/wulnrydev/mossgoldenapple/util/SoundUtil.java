package dev.wulnrydev.mossgoldenapple.util;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Config'ten okunan ses ayarlarini tutan hafif (immutable) veri sinifi.
 * Ses ismi bir kez cozumlenir (parse edilir) ve saklanir — her calmada
 * tekrar enum donusumu yapilmaz. Bu, sik tetiklenen olaylarda gereksiz
 * String islemlerini engeller.
 */
public final class SoundUtil {

    private final boolean enabled;
    private final Sound sound;
    private final float volume;
    private final float pitch;

    private SoundUtil(boolean enabled, Sound sound, float volume, float pitch) {
        this.enabled = enabled;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * Bir config bolumunden ses ayarini olusturur.
     * Gecersiz ses ismi girilirse uyari loglanir ve ses devre disi kalir.
     */
    public static SoundUtil fromSection(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return new SoundUtil(false, null, 1f, 1f);
        }

        boolean enabled = section.getBoolean("enabled", true);
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);

        Sound parsed = null;
        String raw = section.getString("sound", "");
        if (enabled && raw != null && !raw.isEmpty()) {
            try {
                parsed = Sound.valueOf(raw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                logger.warning("Gecersiz ses ismi: '" + raw + "' (" + section.getName()
                        + "). Bu ses devre disi birakildi.");
                enabled = false;
            }
        }

        return new SoundUtil(enabled && parsed != null, parsed, volume, pitch);
    }

    /**
     * Sesi yalnizca ilgili oyuncuya calar (baska oyunculari rahatsiz etmez).
     */
    public void play(Player player) {
        if (enabled && sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
