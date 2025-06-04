package dev.meyi.bn.utilities;

import net.minecraft.util.StringUtils;

/**
 * Utility class for GUI-related string operations and parsing.
 */
public final class GuiStringUtils {
    
    private static final String BAZAAR_TITLE = "Bazaar";
    private static final String PAYMENT_TITLE = "How much do you want to pay?";
    private static final String CONFIRM_BUY_PATTERN = "Confirm Buy Order";
    private static final String CONFIRM_SELL_PATTERN = "Confirm Sell Offer";

    public static boolean isBazaarGui(String displayName) {
        if (displayName == null) {
            return false;
        }
        
        String cleanName = StringUtils.stripControlCodes(displayName);
        
        return cleanName.startsWith(BAZAAR_TITLE) ||
               cleanName.equalsIgnoreCase(PAYMENT_TITLE) ||
               cleanName.matches("Confirm (Buy|Sell) (Order|Offer)") ||
               cleanName.contains(BAZAAR_TITLE);
    }

    public static boolean isPaymentConfirmationGui(String displayName) {
        if (displayName == null) {
            return false;
        }

        String cleanName = StringUtils.stripControlCodes(displayName);
        return cleanName.equalsIgnoreCase(PAYMENT_TITLE);
    }

    public static boolean isOrderConfirmationGui(String displayName) {
        if (displayName == null) {
            return false;
        }

        String cleanName = StringUtils.stripControlCodes(displayName);
        return cleanName.contains(CONFIRM_BUY_PATTERN) || cleanName.contains(CONFIRM_SELL_PATTERN);
    }

    public static String extractOrderType(String displayName) {
        if (displayName == null) {
            return null;
        }

        String cleanName = StringUtils.stripControlCodes(displayName);
        if (cleanName.contains("Buy")) {
            return "Buy";
        } else if (cleanName.contains("Sell")) {
            return "Sell";
        }

        return null;
    }

    public static String cleanDisplayName(String displayName) {
        if (displayName == null) {
            return "";
        }
        return StringUtils.stripControlCodes(displayName);
    }
}
