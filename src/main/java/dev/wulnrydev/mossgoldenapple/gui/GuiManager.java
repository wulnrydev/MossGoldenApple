package dev.wulnrydev.mossgoldenapple.gui;

import dev.wulnrydev.mossgoldenapple.manager.ConfigManager;
import dev.wulnrydev.mossgoldenapple.util.ColorUtil;
import dev.wulnrydev.mossgoldenapple.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Menunun olusturulmasindan, doldurulmasindan ve acilmasindan sorumlu sinif.
 *
 * Dinamik lore ve durum (hazir/yetersiz) hesabi, menu her acildiginda VE her
 * uretimden sonra oyuncunun guncel envanterine gore yapilir. Envanterdeki
 * altin/elma sayimi tek gecerek (InventoryUtil.count) yapildigi icin maliyeti dusuktur.
 *
 * Duzeltmeler:
 *   - fillBackground(): filler ItemStack her slota clone() ile konuluyor (referans
 *     paylasimi kaynaklı tanımsız davranis engellendi).
 *   - parseMaterial(): Material.AIR donus engellendi; hatalı config AIR'e
 *     dusmez, fallback kullanilir.
 *   - open(): CraftHolder'a Logger ataniyor (slot cakisma loglama icin).
 *   - populate(): goldPer/applePer icin savunmaci Math.max(1, ...) eklendi.
 */
public final class GuiManager {

    private final ConfigManager config;
    private final Logger logger;

    public GuiManager(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Yeni bir menu olusturup oyuncuya acar ve acilis sesini calar.
     */
    public void open(Player player) {
        ConfigurationSection gui = config.getGuiSection();
        if (gui == null) {
            logger.severe("GUI bolumu bulunamadi; menu acilamadi. (oyuncu: " + player.getName() + ")");
            return;
        }

        int rows = clamp(gui.getInt("rows", 5), 1, 6);
        int size = rows * 9;
        String title = ColorUtil.colorize(gui.getString("title", "\u00260Altin Elma Uretimi"));

        CraftHolder holder = new CraftHolder();
        holder.setLogger(logger);

        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        populate(inv, holder, player);

        player.openInventory(inv);
        config.getOpenSound().play(player);
    }

    /**
     * Zaten acik olan menuyu, yeniden acmadan (flicker olmadan) tazeler.
     * Uretimden sonra durum/lore/parlama guncellemesi icin kullanilir.
     */
    public void refresh(Inventory inv, CraftHolder holder, Player player) {
        inv.clear();
        holder.clearActions();
        populate(inv, holder, player);
    }

    // ------------------------------------------------------------------
    //  Menuyu doldurma (ortak mantik)
    // ------------------------------------------------------------------

    private void populate(Inventory inv, CraftHolder holder, Player player) {
        ConfigurationSection gui = config.getGuiSection();
        if (gui == null) return;

        int size = inv.getSize();

        // --- Oyuncunun mevcut materyalleri (tek sayim) ---
        PlayerInventory pInv = player.getInventory();
        int haveGold  = InventoryUtil.count(pInv, Material.GOLD_INGOT);
        int haveApple = InventoryUtil.count(pInv, Material.APPLE);

        // Savunmaci clamp: ConfigManager zaten Math.max(1,...) uyguluyor
        // ama burada da garantiye aliyoruz (reload race caution).
        int goldPer  = Math.max(1, config.getGoldPerApple());
        int applePer = Math.max(1, config.getApplePerApple());

        fillBackground(inv, size, gui);
        placeInfoItem(inv, gui, goldPer, applePer, haveGold, haveApple);
        placeOptions(inv, holder, gui, goldPer, applePer, haveGold, haveApple);
        placeCraftAll(inv, holder, gui, goldPer, applePer, haveGold, haveApple);
        placeBackButton(inv, holder, gui);
    }

    // ------------------------------------------------------------------
    //  Bilesenler
    // ------------------------------------------------------------------

    private void fillBackground(Inventory inv, int size, ConfigurationSection gui) {
        ConfigurationSection filler = gui.getConfigurationSection("filler");
        if (filler == null || !filler.getBoolean("enabled", true)) {
            return;
        }
        Material mat = parseMaterial(filler.getString("material"), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack pane = simpleItem(mat, filler.getString("name", "\u00267"), null, false);
        for (int i = 0; i < size; i++) {
            // Her slota bagimsiz bir klon koyulur; ayni referans paylasimi
            // bazi Paper versiyonlarinda beklenmedik davranisa yol acabilir.
            inv.setItem(i, pane.clone());
        }
    }

    private void placeInfoItem(Inventory inv, ConfigurationSection gui,
                               int goldPer, int applePer, int haveGold, int haveApple) {
        ConfigurationSection info = gui.getConfigurationSection("info-item");
        if (info == null || !info.getBoolean("enabled", true)) {
            return;
        }

        Material mat = parseMaterial(info.getString("material"), Material.BOOK);
        List<String> lore = new ArrayList<>(info.getStringList("lore"));
        replaceAll(lore,
                "%gold_per_apple%",  String.valueOf(goldPer),
                "%apple_per_apple%", String.valueOf(applePer),
                "%have_gold%",       String.valueOf(haveGold),
                "%have_apple%",      String.valueOf(haveApple));

        ItemStack item = simpleItem(mat, info.getString("name", "\u0026fBilgi"), lore, false);
        inv.setItem(clampSlot(info.getInt("slot", 4), inv.getSize()), item);
    }

    private void placeOptions(Inventory inv, CraftHolder holder, ConfigurationSection gui,
                              int goldPer, int applePer, int haveGold, int haveApple) {
        ConfigurationSection options = gui.getConfigurationSection("options");
        if (options == null) {
            return;
        }

        List<String> loreTemplate = gui.getStringList("option-lore");
        ConfigurationSection statusSec = gui.getConfigurationSection("status");
        String readyText       = statusSec != null ? statusSec.getString("ready",       "\u0026aHazir")   : "\u0026aHazir";
        String insufficientText = statusSec != null ? statusSec.getString("insufficient", "\u0026cYetersiz") : "\u0026cYetersiz";
        boolean glowWhenReady  = statusSec == null || statusSec.getBoolean("glow-when-ready", true);

        for (String key : options.getKeys(false)) {
            ConfigurationSection opt = options.getConfigurationSection(key);
            if (opt == null || !opt.getBoolean("enabled", true)) {
                continue;
            }

            int produce   = Math.max(1, opt.getInt("produce", 1));
            int goldCost  = produce * goldPer;
            int appleCost = produce * applePer;
            boolean ready = haveGold >= goldCost && haveApple >= appleCost;

            List<String> lore = new ArrayList<>(loreTemplate);
            replaceAll(lore,
                    "%produce%",    String.valueOf(produce),
                    "%gold_cost%",  String.valueOf(goldCost),
                    "%apple_cost%", String.valueOf(appleCost),
                    "%have_gold%",  String.valueOf(haveGold),
                    "%have_apple%", String.valueOf(haveApple),
                    "%status%",     ready ? readyText : insufficientText);

            Material icon = parseMaterial(opt.getString("icon"), Material.GOLDEN_APPLE);
            ItemStack item = simpleItem(icon, opt.getString("name", "\u00266Altin Elma"), lore, ready && glowWhenReady);

            int slot = clampSlot(opt.getInt("slot"), inv.getSize());
            inv.setItem(slot, item);
            holder.mapSlot(slot, produce);
        }
    }

    private void placeCraftAll(Inventory inv, CraftHolder holder, ConfigurationSection gui,
                               int goldPer, int applePer, int haveGold, int haveApple) {
        ConfigurationSection all = gui.getConfigurationSection("craft-all");
        if (all == null || !all.getBoolean("enabled", true)) {
            return;
        }

        // Matematiksel maksimum: iki kaynaktan hangisi once biterse o sinirlar.
        int craftable = Math.min(haveGold / goldPer, haveApple / applePer);
        boolean ready = craftable > 0;

        ConfigurationSection statusSec = gui.getConfigurationSection("status");
        boolean glowWhenReady = statusSec == null || statusSec.getBoolean("glow-when-ready", true);

        List<String> lore = new ArrayList<>(all.getStringList("lore"));
        replaceAll(lore, "%craftable%", String.valueOf(craftable));

        Material icon = parseMaterial(all.getString("icon"), Material.GOLD_BLOCK);
        ItemStack item = simpleItem(icon, all.getString("name", "\u0026aHepsini Yap"), lore, ready && glowWhenReady);

        int slot = clampSlot(all.getInt("slot"), inv.getSize());
        inv.setItem(slot, item);
        holder.mapSlot(slot, CraftHolder.CRAFT_ALL);
    }

    private void placeBackButton(Inventory inv, CraftHolder holder, ConfigurationSection gui) {
        ConfigurationSection back = gui.getConfigurationSection("back-button");
        if (back == null || !back.getBoolean("enabled", true)) {
            return;
        }

        Material icon = parseMaterial(back.getString("icon"), Material.ARROW);
        ItemStack item = simpleItem(icon, back.getString("name", "\u0026cAna Menu"), back.getStringList("lore"), false);

        int slot = clampSlot(back.getInt("slot"), inv.getSize());
        inv.setItem(slot, item);
        holder.mapSlot(slot, CraftHolder.BACK_BUTTON);
        // Tiklaninca oyuncu adina calistirilacak komut.
        holder.setBackCommand(back.getString("command", "anamenu"));
    }

    // ------------------------------------------------------------------
    //  Yardimcilar
    // ------------------------------------------------------------------

    /**
     * Isim, lore ve istege bagli parlama (glow) ile hafif bir ItemStack olusturur.
     */
    private ItemStack simpleItem(Material material, String name, List<String> lore, boolean glow) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            // Material.AIR icin meta null gelir; parseMaterial bunu zaten engeller
            // ama savunmaci olarak orijinal (isim/loresiz) stack dondurulur.
            return stack;
        }

        if (name != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
        }
        if (lore != null && !lore.isEmpty()) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) {
                colored.add(ColorUtil.colorize(line));
            }
            meta.setLore(colored);
        }
        if (glow) {
            // Parlama efekti: gorunmez bir enchant + enchant yazisini gizleme.
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        stack.setItemMeta(meta);
        return stack;
    }

    private void replaceAll(List<String> lore, String... pairs) {
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            for (int p = 0; p + 1 < pairs.length; p += 2) {
                if (line.contains(pairs[p])) {
                    line = line.replace(pairs[p], pairs[p + 1]);
                }
            }
            lore.set(i, line);
        }
    }

    /**
     * Ham string'i Material'e donusturur.
     * Gecersiz veya AIR materyal fallback'e dusuler; Material.AIR GUI icin anlamsizdir.
     */
    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        Material m = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
        // Material.AIR'i reddet: meta null gelerek isim/lore uygulanamaz.
        if (m == null || m == Material.AIR) {
            return fallback;
        }
        return m;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampSlot(int slot, int size) {
        if (slot < 0)    return 0;
        if (slot >= size) return size - 1;
        return slot;
    }
}
