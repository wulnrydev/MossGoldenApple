package dev.wulnrydev.mossgoldenapple.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renk donusturme yardimcisi.
 * Hem standart renk kodlarini (&7, &c ...) hem de HEX kodlarini (&#RRGGBB) destekler.
 *
 * NOT: Pattern nesnesi tek sefer derlenir (static final) — her cagride yeniden
 * derlenmedigi icin performans acisindan optimaldir.
 *
 * Degisiklik: net.md_5.bungee.api.ChatColor (deprecated) kaldirildi.
 * HEX kodlari artik Spigot/Paper'in anlayacagi §x§R§R§G§G§B§B formatina
 * donusturulur; standart '&' kodlari icin org.bukkit.ChatColor kullanilir.
 * StringBuilder (synchronized degil) StringBuffer yerine tercih edilir (Java 17+).
 */
public final class ColorUtil {

    // &#RRGGBB formatini yakalar. Onceden derlenir, tekrar kullanilir.
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {
        // Yardimci sinif — ornek olusturulamaz.
    }

    /**
     * Verilen metindeki tum renk kodlarini (HEX + standart) oyuncunun gorecegi
     * renklere donusturur.
     *
     * @param input ham metin (null gonderilirse bos string doner)
     * @return renklendirilmis metin
     */
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // 1) Once HEX kodlarini isle: &#RRGGBB -> §x§R§R§G§G§B§B
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            // Regex eşleşmesinde özel karakterler olabileceğinden replaceAll yerine
            // appendReplacement + literalReplacement kullanıyoruz.
            matcher.appendReplacement(buffer, hexToLegacy(hex));
        }
        matcher.appendTail(buffer);

        // 2) Ardindan standart '&' kodlarini isle.
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * "RRGGBB" bicimindeki hex dizisini Spigot/Paper'in anlayacagi
     * §x§R§R§G§G§B§B (12-karakter legacy format) seklinde dondurur.
     * Hicbir dis bagimliligi yoktur.
     */
    private static String hexToLegacy(String hex) {
        // 2 + 2*6 = 14 karakter kapasitesi (§x + 6 adet §X)
        StringBuilder sb = new StringBuilder(14);
        sb.append('\u00A7').append('x');
        for (char c : hex.toCharArray()) {
            sb.append('\u00A7').append(c);
        }
        return sb.toString();
    }
}
