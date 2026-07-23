package dev.wulnrydev.mossgoldenapple.listener;

import dev.wulnrydev.mossgoldenapple.gui.CraftHolder;
import dev.wulnrydev.mossgoldenapple.gui.GuiManager;
import dev.wulnrydev.mossgoldenapple.manager.ConfigManager;
import dev.wulnrydev.mossgoldenapple.util.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Menu icindeki tiklamalari yoneten listener.
 *
 * Performans/guvenlik: Menu bize aitse tum tiklama daima iptal edilir; boylece
 * oyuncu esyalari disari alamaz veya shift-click ile tasiyamaz. Aksiyon araminda
 * config'e tekrar bakilmaz — slot->aksiyon haritasi CraftHolder'da hazirdir.
 *
 * Duzeltmeler:
 *   - @EventHandler: EventPriority.HIGH + ignoreCancelled=true eklendi.
 *     Baska bir plugin NORMAL oncelikte islemi iptal etse bile biz devreye gireriz;
 *     baska bir plugin HIGHEST'ta iptal kaldirsa bizim islemi etkilemez.
 *   - Spam-click / duplicate korumasi: clickCooldown (zaman damgali) ile
 *     ayni oyuncunun COOLDOWN_MS icerisinde ikinci tiklarini reddeder.
 *     Tum event'lar ayni main-thread'de islense de istemci hizli tiklayinca
 *     sunucu envanter guncellenmeden birden fazla paket gonderebilir.
 *   - performCommand() oncesinde komut sanitasyonu (;, newline, carriage-return
 *     karakterleri temizleniyor).
 *   - InventoryCloseEvent / PlayerQuitEvent: cooldown haritasindan temizlik.
 */
public final class GuiListener implements Listener {

    /** Ayni oyuncunun iki tiklama arasinda gecmesi gereken minimum sure (ms). */
    private static final long COOLDOWN_MS = 200L;

    private final ConfigManager config;
    private final GuiManager guiManager;

    /**
     * Oyuncu UUID -> son tiklama zaman damgasi (ms).
     * Main-thread'de erisildigi icin ConcurrentHashMap gerekmiyor.
     */
    private final Map<UUID, Long> clickCooldown = new HashMap<>();

    public GuiListener(ConfigManager config, GuiManager guiManager) {
        this.config = config;
        this.guiManager = guiManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        // Hizli filtre: menu bize ait degilse hicbir sey yapma.
        if (!(top.getHolder() instanceof CraftHolder holder)) {
            return;
        }

        // Menu bizim — her turlu tiklamayi iptal et (esya calinmasini engelle).
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Yalnizca ust (menu) envanterindeki tiklamalarla ilgileniyoruz.
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return;
        }

        Integer action = holder.actionAt(rawSlot);
        if (action == null) {
            return; // Cam panel / bilgi esyasi vb. — sadece iptal edildi.
        }

        // --- Spam-click / duplicate korumasi ---
        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastClick = clickCooldown.get(uid);
        if (lastClick != null && (now - lastClick) < COOLDOWN_MS) {
            return; // Cok hizli tiklama — sessizce reddet.
        }
        clickCooldown.put(uid, now);

        // --- Ana Menu butonu ---
        if (action == CraftHolder.BACK_BUTTON) {
            player.closeInventory();
            String cmd = holder.getBackCommand();
            if (cmd != null && !cmd.isEmpty()) {
                // Oyuncu adina komut calistir (basindaki '/' varsa temizlenir).
                // Guvenlik: ';', newline, carriage-return gibi injection karakterleri
                // temizlenerek tek bir komutun calistirilmasi garanti altina alinir.
                String safeCmd = cmd.replaceAll("[;\n\r|]", "").trim();
                if (!safeCmd.isEmpty()) {
                    player.performCommand(safeCmd.startsWith("/") ? safeCmd.substring(1) : safeCmd);
                }
            }
            return;
        }

        // --- Uretim islemleri ---
        int goldPer  = config.getGoldPerApple();
        int applePer = config.getApplePerApple();

        PlayerInventory pInv = player.getInventory();
        int haveGold  = InventoryUtil.count(pInv, Material.GOLD_INGOT);
        int haveApple = InventoryUtil.count(pInv, Material.APPLE);

        int produce;
        if (action == CraftHolder.CRAFT_ALL) {
            // Uretilebilecek maksimum: kaynaklardan hangisi once biterse.
            produce = Math.min(haveGold / goldPer, haveApple / applePer);
        } else {
            produce = action; // Sabit miktar (1, 8, 16, 32, 64 ...)
        }

        int goldCost  = produce * goldPer;
        int appleCost = produce * applePer;

        // Yetersiz materyal veya gecersiz miktar -> hata.
        if (produce <= 0 || haveGold < goldCost || haveApple < appleCost) {
            config.getFailSound().play(player);
            config.send(player, "not-enough",
                    "%gold_cost%",  String.valueOf(Math.max(goldCost,  goldPer)),
                    "%apple_cost%", String.valueOf(Math.max(appleCost, applePer)));
            return;
        }

        // Materyalleri dus, altin elmalari ver.
        InventoryUtil.remove(pInv, Material.GOLD_INGOT, goldCost);
        InventoryUtil.remove(pInv, Material.APPLE,      appleCost);

        boolean dropped = giveGoldenApples(player, produce);

        config.getSuccessSound().play(player);
        config.send(player, "craft-success", "%amount%", String.valueOf(produce));
        if (dropped) {
            config.send(player, "inventory-full");
        }

        // Menuyu guncel envantere gore tazele (durum/lore/parlama).
        guiManager.refresh(top, holder, player);
        player.updateInventory();
    }

    /**
     * Oyuncu menuden cikarken cooldown haritasindan kaydi temizle.
     * Bellek sizintisini onler.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CraftHolder) {
            clickCooldown.remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Oyuncu sunucudan ayrilirken cooldown haritasindan kaydi temizle.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clickCooldown.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Uretilen altin elmalari, 64'luk yiginlar halinde oyuncuya verir.
     * @return envanter dolu oldugu icin yere birakma yapildiysa true
     */
    private boolean giveGoldenApples(Player player, int amount) {
        boolean dropped = false;
        int remaining = amount;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, 64);
            ItemStack apples = new ItemStack(Material.GOLDEN_APPLE, stackSize);
            if (InventoryUtil.give(player, apples)) {
                dropped = true;
            }
            remaining -= stackSize;
        }
        return dropped;
    }
}
