package dev.wulnrydev.mossgoldenapple.listener;

import dev.wulnrydev.mossgoldenapple.manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Recipe;

/**
 * Vanilla altin elma craftini engelleyen listener.
 *
 * Performans: Her iki olayda da EN BASTA sonuc materyaline bakilir. Sonuc
 * GOLDEN_APPLE degilse hicbir islem yapilmadan aninda return edilir. Bu,
 * sunucudaki DIGER tum craft islemleri icin maliyeti neredeyse sifir yapar.
 */
public final class CraftBlockListener implements Listener {

    private final ConfigManager config;

    public CraftBlockListener(ConfigManager config) {
        this.config = config;
    }

    /**
     * Craft masasinda sonucu onceden gizler — sonuc slotunda altin elma gorunmez.
     * (Cift katmanli koruma: gorsel olarak da uretimi engeller.)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!config.isBlockVanillaCraft()) {
            return;
        }
        Recipe recipe = event.getRecipe();
        // Hizli filtre: ilgili degilse cik.
        if (recipe == null || recipe.getResult().getType() != Material.GOLDEN_APPLE) {
            return;
        }
        event.getInventory().setResult(null);
    }

    /**
     * Oyuncu yine de craftlamaya calisirsa islemi iptal eder ve uyari gonderir.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!config.isBlockVanillaCraft()) {
            return;
        }
        Recipe recipe = event.getRecipe();
        // Hizli filtre: ilgili degilse cik.
        if (recipe == null || recipe.getResult().getType() != Material.GOLDEN_APPLE) {
            return;
        }

        event.setCancelled(true);

        HumanEntity clicker = event.getWhoClicked();
        if (clicker instanceof Player player) {
            player.sendMessage(config.message("vanilla-blocked"));
            config.getFailSound().play(player);
        }
    }
}
