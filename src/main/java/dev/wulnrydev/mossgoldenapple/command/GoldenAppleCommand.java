package dev.wulnrydev.mossgoldenapple.command;

import dev.wulnrydev.mossgoldenapple.gui.CraftHolder;
import dev.wulnrydev.mossgoldenapple.gui.GuiManager;
import dev.wulnrydev.mossgoldenapple.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * /altinelma komutu (aliases: goldenapple, gapple).
 * Yalnizca oyuncular kullanabilir; menuyu acar.
 * "reload" alt komutu config'i yeniden yukler (yetki gerektirir).
 *
 * Duzeltmeler:
 *   - TabCompleter implemente edildi: "reload" alt komutu için sekme tamamlama.
 *   - Reload sonrasında açık tüm MossGoldenApple menüleri kapatılır; eski
 *     config verisiyle kalan menülerin stale gösterim yapması engellenir.
 */
public final class GoldenAppleCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final GuiManager guiManager;

    public GoldenAppleCommand(ConfigManager config, GuiManager guiManager) {
        this.config = config;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Reload alt komutu — konsoldan da calisabilir.
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mossgoldenapple.reload")) {
                sender.sendMessage(config.message("no-permission"));
                return true;
            }

            // Reload oncesinde acik tum menuleri kapat (stale veri goruntulenmesini engelle).
            closeAllOpenMenus();

            config.reload();
            sender.sendMessage(config.message("reloaded"));
            return true;
        }

        // Menu yalnizca oyunculara acilir.
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("players-only"));
            return true;
        }

        if (!player.hasPermission("mossgoldenapple.use")) {
            player.sendMessage(config.message("no-permission"));
            return true;
        }

        guiManager.open(player);
        return true;
    }

    /**
     * Sekme tamamlama: "reload" alt komutunu yalnizca yetkili gonderenlere gosterir.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("mossgoldenapple.reload")) {
            String partial = args[0].toLowerCase();
            if ("reload".startsWith(partial)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }

    /**
     * Sunucudaki tum oyunculardan MossGoldenApple menusunu acik olanlari kapatir.
     * Reload sirasinda cagrilir; menunun eski config verisiyle acik kalmasi onlenir.
     */
    private void closeAllOpenMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getOpenInventory().getTopInventory().getHolder() instanceof CraftHolder) {
                online.closeInventory();
            }
        }
    }
}
