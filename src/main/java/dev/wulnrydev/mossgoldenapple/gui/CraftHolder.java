package dev.wulnrydev.mossgoldenapple.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Menuye ait ozel InventoryHolder.
 *
 * Bu sinif sayesinde tiklama olayinda menunun bize ait olup olmadigini
 * pahali baslik (title) karsilastirmasi yerine tek 'instanceof' ile
 * anlariz. Ayrica hangi slotun hangi uretimi/aksiyonu tetikledigini onceden
 * haritalayarak, tiklamada config'i tekrar okumaya gerek kalmaz.
 *
 * Duzeltme: mapSlot() artik slot cakismasini tespit edip log uyarisi verir.
 */
public final class CraftHolder implements InventoryHolder {

    /** "Hepsini Yap" butonunu temsil eden ozel deger. */
    public static final int CRAFT_ALL = -1;
    /** "Ana Menu" (geri don) butonunu temsil eden ozel deger. */
    public static final int BACK_BUTTON = -2;

    // slot -> uretilecek adet (veya CRAFT_ALL / BACK_BUTTON)
    private final Map<Integer, Integer> actionSlots = new HashMap<>();

    // Ana menu butonuna basildiginda oyuncu adina calistirilacak komut.
    private String backCommand = "anamenu";

    private Inventory inventory;

    // Cakisma uyarilari icin optional logger (null ise sessiz calisir).
    private Logger logger;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Verilen slotu bir aksiyona esler.
     * Slot zaten farkli bir aksiyona atanmissa log uyarisi verilir.
     */
    public void mapSlot(int slot, int action) {
        Integer existing = actionSlots.put(slot, action);
        if (existing != null && existing != action && logger != null) {
            logger.warning("[MossGoldenApple] GUI slot cakismasi tespit edildi: slot "
                    + slot + " — onceki aksiyon=" + existing + ", yeni aksiyon=" + action
                    + ". config.yml'deki slot atamalarini kontrol edin.");
        }
    }

    /**
     * Verilen slotta bir aksiyon var mi?
     * @return uretilecek adet, CRAFT_ALL, BACK_BUTTON, veya null (aksiyon yoksa)
     */
    public Integer actionAt(int slot) {
        return actionSlots.get(slot);
    }

    /** Menu tazelenirken eski slot haritasini temizler. */
    public void clearActions() {
        actionSlots.clear();
    }

    public String getBackCommand() {
        return backCommand;
    }

    public void setBackCommand(String backCommand) {
        this.backCommand = backCommand;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
