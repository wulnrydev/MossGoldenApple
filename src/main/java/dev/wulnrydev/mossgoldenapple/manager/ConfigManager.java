package dev.wulnrydev.mossgoldenapple.manager;

import dev.wulnrydev.mossgoldenapple.MossGoldenApple;
import dev.wulnrydev.mossgoldenapple.util.ColorUtil;
import dev.wulnrydev.mossgoldenapple.util.SoundUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Config yonetimi merkezi.
 *
 * Performans notu: Sik erisilen sabit degerler (maliyet, sesler, gui bolumu)
 * reload aninda tek sefer okunup burada tutulur. Boylece olay/tik icinde
 * tekrarli config okumasi ve String->enum donusumu yapilmaz.
 *
 * Duzeltme: guiSection artik reload'da onbellekleniyor (her cagirda yeni
 * ConfigurationSection nesnesi uretilmiyordu). Ayrica config-version
 * uyumsuzlugu log uyarisiyla raporlaniyor.
 */
public final class ConfigManager {

    /** Desteklenen config versiyonu. */
    private static final int SUPPORTED_CONFIG_VERSION = 1;

    private final MossGoldenApple plugin;

    // Onbelleklenen (cache) sabit degerler.
    private int goldPerApple;
    private int applePerApple;
    private boolean blockVanillaCraft;

    private SoundUtil openSound;
    private SoundUtil successSound;
    private SoundUtil failSound;

    private String prefix;

    // GUI bolumu: reload aninda bir kez okunur, tekrar tekrar sorgulanmaz.
    private ConfigurationSection guiSection;

    public ConfigManager(MossGoldenApple plugin) {
        this.plugin = plugin;
    }

    /**
     * Config'i disk'ten (yeniden) yukler ve onbellegi tazeler.
     */
    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        // Config versiyonu kontrolu — eski config uyarsiz ise operatoru bilgilendir.
        int foundVersion = cfg.getInt("config-version", 0);
        if (foundVersion != SUPPORTED_CONFIG_VERSION) {
            plugin.getLogger().warning(
                    "config.yml versiyon uyumsuzlugu! Beklenen: " + SUPPORTED_CONFIG_VERSION
                    + ", bulunan: " + foundVersion
                    + ". Lutfen config.yml'yi guncel varsayilan ile karsilastirin; "
                    + "eksik anahtarlar varsayilan degerlere dusecektir.");
        }

        this.goldPerApple  = Math.max(1, cfg.getInt("recipe.gold-per-apple",  18));
        this.applePerApple = Math.max(1, cfg.getInt("recipe.apple-per-apple", 1));
        this.blockVanillaCraft = cfg.getBoolean("block-vanilla-craft", true);

        this.openSound    = SoundUtil.fromSection(cfg.getConfigurationSection("sounds.open"),    plugin.getLogger());
        this.successSound = SoundUtil.fromSection(cfg.getConfigurationSection("sounds.success"), plugin.getLogger());
        this.failSound    = SoundUtil.fromSection(cfg.getConfigurationSection("sounds.fail"),    plugin.getLogger());

        this.prefix     = cfg.getString("messages.prefix", "");
        this.guiSection = cfg.getConfigurationSection("gui");

        if (this.guiSection == null) {
            plugin.getLogger().severe("config.yml icinde 'gui' bolumu bulunamadi! "
                    + "Menu olusturulamayabilir; lutfen varsayilan config'i yenileyin.");
        }
    }

    // ---------------- Kisa erisim yardimcilari ----------------

    public FileConfiguration raw() {
        return plugin.getConfig();
    }

    public int getGoldPerApple() {
        return goldPerApple;
    }

    public int getApplePerApple() {
        return applePerApple;
    }

    public boolean isBlockVanillaCraft() {
        return blockVanillaCraft;
    }

    public SoundUtil getOpenSound() {
        return openSound;
    }

    public SoundUtil getSuccessSound() {
        return successSound;
    }

    public SoundUtil getFailSound() {
        return failSound;
    }

    /**
     * Onbelleklenmis GUI bolumunu dondurur.
     * Her cagirda yeni nesne uretilmez — reload aninda hazirlanmistir.
     */
    public ConfigurationSection getGuiSection() {
        return guiSection;
    }

    /**
     * messages.* altindaki bir mesaji, prefix + renk uygulanmis olarak dondurur.
     */
    public String message(String path) {
        String raw = plugin.getConfig().getString("messages." + path, "");
        return ColorUtil.colorize(prefix + raw);
    }

    /**
     * Placeholder destekli mesaj gonderimi.
     * Argumanlar {anahtar, deger, anahtar, deger, ...} sirasindadir.
     */
    public void send(Player player, String path, String... replacements) {
        String raw = plugin.getConfig().getString("messages." + path, "");
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String full = prefix + raw;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            full = full.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(ColorUtil.colorize(full));
    }
}
