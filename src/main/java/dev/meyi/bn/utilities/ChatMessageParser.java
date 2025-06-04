package dev.meyi.bn.utilities;

import dev.meyi.bn.BazaarNotifier;
import dev.meyi.bn.json.Order;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing complex chat messages from the Bazaar.
 * Centralizes string parsing logic to improve readability and maintainability.
 */
public final class ChatMessageParser {
    private static final Pattern ORDER_SETUP_PATTERN = Pattern.compile(
            "(?:\\[Bazaar] )?(Buy|Sell) (Order|Offer) Setup! ([\\d,]+)x (.+?) for ([\\d,.]+) coins\\.");

    private static final Pattern ORDER_FILLED_PATTERN = Pattern.compile(
            "\\[Bazaar] Your (Buy Order|Sell Offer) for (\\d+)x (.*) was filled!");

    private static final Pattern SELL_OFFER_PATTERN = Pattern.compile(
            "\\[Bazaar] Claimed .* coins from selling (.*)x (.*) at (.*) each!");

    private static final Pattern BUY_ORDER_PATTERN = Pattern.compile(
            "\\[Bazaar] Claimed (.*)x (.*) worth .* coins bought for (.*) each!");

    private static final Pattern INSTANT_SELL_PATTERN = Pattern.compile(
            "\\[Bazaar] Sold (.*)x (.*) for (.*) coins!");

    private static final Pattern INSTANT_BUY_PATTERN = Pattern.compile(
            "\\[Bazaar] Bought (.*)x (.*) for (.*) coins!");

    private static final Pattern CANCELLATION_BUY_PATTERN = Pattern.compile(
            "(?:\\[Bazaar] )?Cancelled! Refunded ([\\d,.]+) coins from cancelling Buy Order!");

    private static final Pattern CANCELLATION_SELL_PATTERN = Pattern.compile(
            "(?:\\[Bazaar] )?Cancelled! Refunded ([\\d,]+)x (.*) from cancelling Sell Offer!");


    public static Order parseTransaction(String message) {
        Matcher matcher;
        
        if ((matcher = SELL_OFFER_PATTERN.matcher(message)).find()) {
            int amount = parseAmount(matcher.group(1));
            String displayName = matcher.group(2);
            String itemName = BazaarNotifier.bazaarConv.inverse().get(displayName);
            if (itemName == null) {
                return null;
            }
            double pricePerUnit = parsePrice(matcher.group(3));
            return new Order(itemName, Order.OrderType.SELL, pricePerUnit, amount);
        }
        
        if ((matcher = INSTANT_SELL_PATTERN.matcher(message)).find()) {
            int amount = parseAmount(matcher.group(1));
            String displayName = matcher.group(2);
            String itemName = BazaarNotifier.bazaarConv.inverse().get(displayName);
            if (itemName == null) {
                return null;
            }
            double totalCoins = parsePrice(matcher.group(3));
            double pricePerUnit = totalCoins / amount;
            return new Order(itemName, Order.OrderType.SELL, pricePerUnit, amount);
        }
        
        if ((matcher = BUY_ORDER_PATTERN.matcher(message)).find()) {
            int amount = parseAmount(matcher.group(1));
            String displayName = matcher.group(2);
            String itemName = BazaarNotifier.bazaarConv.inverse().get(displayName);
            if (itemName == null) {
                return null;
            }
            double pricePerUnit = parsePrice(matcher.group(3));
            return new Order(itemName, Order.OrderType.BUY, pricePerUnit, amount);
        }
        
        if ((matcher = INSTANT_BUY_PATTERN.matcher(message)).find()) {
            int amount = parseAmount(matcher.group(1));
            String displayName = matcher.group(2);
            String itemName = BazaarNotifier.bazaarConv.inverse().get(displayName);
            if (itemName == null) {
                return null;
            }
            double totalCoins = parsePrice(matcher.group(3));
            double pricePerUnit = totalCoins / amount;
            return new Order(itemName, Order.OrderType.BUY, pricePerUnit, amount);
        }
        
        return null;
    }


    public static Order parseOrderSetup(String message) {
        Matcher matcher = ORDER_SETUP_PATTERN.matcher(message);
        if (matcher.find()) {
            Order.OrderType orderType = matcher.group(1).equals("Buy") ? Order.OrderType.BUY : Order.OrderType.SELL;
            int amount = parseAmount(matcher.group(3));
            String displayName = matcher.group(4);
            String itemName = BazaarNotifier.bazaarConv.inverse().get(displayName); // Convert display name to product ID
            if (itemName == null) {
                return null; // Or handle this error appropriately
            }
            double totalPrice = parsePrice(matcher.group(5));
            double pricePerUnit = totalPrice / amount; // Calculate price per unit
            return new Order(itemName, orderType, pricePerUnit, amount);
        }
        return null;
    }


    public static Order parseOrderFilled(String message) {
        Matcher matcher = ORDER_FILLED_PATTERN.matcher(message);
        if (matcher.find()) {
            Order.OrderType type = matcher.group(1).equals("Buy Order") ? Order.OrderType.BUY : Order.OrderType.SELL;
            int amount = parseAmount(matcher.group(2));
            String itemName = BazaarNotifier.bazaarConv.inverse().get(matcher.group(3));
            // pricePerUnit is not available in OrderFilledInfo, setting to 0
            return new Order(itemName, type, 0, amount);
        }
        return null;
    }

    public static Order parseCancellation(String message) {
        Matcher matcher;
        
        if ((matcher = CANCELLATION_BUY_PATTERN.matcher(message)).find()) {
            double refundCoins = parsePrice(matcher.group(1));
            // For cancellation, amount and pricePerUnit are not directly available for the original order.
            // Setting amount to 0 and pricePerUnit to refundCoins for now, as a placeholder.
            return new Order("", Order.OrderType.BUY, refundCoins, 0);
        }
        
        if ((matcher = CANCELLATION_SELL_PATTERN.matcher(message)).find()) {
            int refundAmount = parseAmount(matcher.group(1));
            String itemName = BazaarNotifier.bazaarConv.inverse().get(matcher.group(2)); // Convert to logical name
            // For cancellation, pricePerUnit is not directly available for the original order.
            // Setting pricePerUnit to 0.
            return new Order(itemName, Order.OrderType.SELL, 0, refundAmount);
        }
        
        return null;
    }


    private static int parseAmount(String amountStr) {
        return Integer.parseInt(amountStr.replaceAll("[,.]", ""));
    }

    private static double parsePrice(String priceStr) {
        return Double.parseDouble(priceStr.replaceAll(",", ""));
    }
}
