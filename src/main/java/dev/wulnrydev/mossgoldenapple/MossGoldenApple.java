package dev.wulnrydev.mossgoldenapple;

import dev.wulnrydev.mossgoldenapple.command.GoldenAppleCommand;
import dev.wulnrydev.mossgoldenapple.gui.GuiManager;
import dev.wulnrydev.mossgoldenapple.listener.CraftBlockListener;
import dev.wulnrydev.mossgoldenapple.listener.GuiListener;
import dev.wulnrydev.mossgoldenapple.manager.ConfigManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MossGoldenApple — ana eklenti sinifi.
 *
 * Yapimci: wulnrydev
 * Hedef  : Paper/Spigot 1.20+ (Towny, yuksek oyunculu, performans-kritik)
 *
 * Mimari:
 *   - ConfigManager     : config yukleme + onbellek + mesaj/ses erisimi
 *   - GuiManager        : menu olusturma / doldurma / tazeleme
 *   - CraftBlockListener : vanilla altin elma craftini engeller
 *   - GuiListener       : menu tiklamalari ve uretim mantigi
 *   - GoldenAppleCommand : /altinelma komutu + sekme tamamlama
 */
public final class MossGoldenApple extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Varsayilan config.yml'i (yoksa) diske yaz.
        saveDefaultConfig();

        // Yoneticileri hazirla.
        this.configManager = new ConfigManager(this);
        this.configManager.reload();

        // GuiManager artik slot-cakisma loglamasi icin Logger aliyor.
        GuiManager guiManager = new GuiManager(configManager, getLogger());

        // Listener'lari kaydet.
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new CraftBlockListener(configManager), this);
        pm.registerEvents(new GuiListener(configManager, guiManager), this);

        // Komutu kaydet (executor + tab completer ayni nesne).
        PluginCommand command = getCommand("altinelma");
        if (command != null) {
            GoldenAppleCommand cmd = new GoldenAppleCommand(configManager, guiManager);
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        } else {
            getLogger().severe("'altinelma' komutu plugin.yml icinde bulunamadi! Komut devre disi.");
        }

        getLogger().info("MossGoldenApple etkinlestirildi. (yapimci: wulnrydev)");
    }

    @Override
    public void onDisable() {
        getLogger().info("MossGoldenApple devre disi birakildi.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
