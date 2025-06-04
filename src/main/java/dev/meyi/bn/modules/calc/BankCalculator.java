package dev.meyi.bn.modules.calc;

import dev.meyi.bn.BazaarNotifier;
import dev.meyi.bn.json.Exchange;
import dev.meyi.bn.json.Order;
import dev.meyi.bn.json.Order.OrderType;
import dev.meyi.bn.utilities.ChatMessageParser;
import dev.meyi.bn.utilities.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class BankCalculator {

  private static final List<Exchange> orderHistory = new ArrayList<>();
  private static double rawDifference = 0;
  public static double getRawDifference() {
    return rawDifference;
  }

  public static void calculateBazaarProfit() {
    for (int i = orderHistory.size() - 1; i >= 0; i--) {
      Exchange sell = orderHistory.get(i);
      if (sell.getAmount() != 0 && sell.getType() == OrderType.SELL) {
        for (int j = orderHistory.size() - 1; j >= 0; j--) {
          Exchange buy = orderHistory.get(j);
          if (buy.getAmount() != 0 && sell.matchesOrder(buy)) {
            if (buy.getAmount() >= sell.getAmount()) {
              BazaarNotifier.config.bankModule.bazaarProfit +=
                  sell.getAmount() * (sell.getPricePerUnit() * .99 - buy.getPricePerUnit());
              buy.removeAmount(sell.getAmount());
              sell.removeAmount(sell.getAmount());
            } else {
              BazaarNotifier.config.bankModule.bazaarProfit  +=
                  buy.getAmount() * (sell.getPricePerUnit() * .99 - buy.getPricePerUnit());
              sell.removeAmount(buy.getAmount());
              buy.removeAmount(buy.getAmount());
            }

            if (sell.getAmount() == 0) {
              break;
            }
          }
        }
      }
    }

    orderHistory.removeIf(v -> v.getAmount() == 0);

    craftingLoop:
    for (int i = orderHistory.size() - 1; i >= 0; i--) {
      if (orderHistory.get(i).getAmount() != 0 && orderHistory.get(i).canCraft()) {
        Map<String, Integer> craftingResources = orderHistory.get(i).getCraftingResources();
        Map<String, List<Exchange>> availableResources = new HashMap<>();
        craftingResources.keySet().forEach(key -> availableResources.put(key, new ArrayList<>()));
        for (Exchange exchange : orderHistory) {
          if (exchange.getType() == OrderType.BUY && craftingResources.containsKey(
              exchange.productID)) {
            availableResources.get(exchange.productID).add(exchange);
          }
        }

        int maxCrafting = orderHistory.get(i).getAmount();

        for (Entry<String, List<Exchange>> entry : availableResources.entrySet()) {
          int amountAvailable = 0;
          for (Exchange e : entry.getValue()) {
            amountAvailable += e.getAmount();
          }

          int craftCost = craftingResources.get(entry.getKey());

          if (amountAvailable < craftCost) {
            break craftingLoop;
          } else {
            maxCrafting = Math.min(maxCrafting, amountAvailable / craftCost);
          }
        }

        double buyValue = 0;

        if (maxCrafting > 0) {
          for (String key : availableResources.keySet()) {
            int valueToRemove = maxCrafting * craftingResources.get(key);
            for (Exchange e : availableResources.get(key)) {
              if (e.getAmount() >= valueToRemove) {
                buyValue += valueToRemove * e.getPricePerUnit();
                e.removeAmount(valueToRemove);
                valueToRemove = 0;
              } else {
                buyValue += e.getAmount() * e.getPricePerUnit();
                valueToRemove -= e.getAmount();
                e.removeAmount(e.getAmount());
              }
            }
          }
        }
        orderHistory.get(i).removeAmount(maxCrafting);
        BazaarNotifier.config.bankModule.bazaarProfit  +=
            ((double) maxCrafting * orderHistory.get(i).getPricePerUnit()) * .99 - buyValue;
      }
    }

    orderHistory.removeIf(v -> v.getAmount() == 0);
  }

  public static void evaluate(String message) {
    Order transactionOrder = ChatMessageParser.parseTransaction(message);
    
    if (transactionOrder == null) {
      return; // Message doesn't match any transaction pattern
    }
    
    String productId = transactionOrder.productID;
    
    if (!productId.isEmpty()) {
      Exchange exchange = new Exchange(transactionOrder.type, productId, transactionOrder.pricePerUnit, transactionOrder.startAmount);
      
      addOrUpdateExchange(exchange);
      
      updateRawDifference(exchange);
      
      calculateBazaarProfit();
    }
  }

  private static void addOrUpdateExchange(Exchange exchange) {
    int existingIndex = orderHistory.indexOf(exchange);
    if (existingIndex != -1) {
      orderHistory.get(existingIndex).addAmount(exchange.getAmount());
    } else {
      orderHistory.add(exchange);
    }
  }

  private static void updateRawDifference(Exchange exchange) {
    double multiplier = (exchange.getType() == OrderType.BUY) ? -1.0 : 0.99;
    rawDifference += multiplier * exchange.getPricePerUnit() * exchange.getAmount();
  }

  public static void evaluateCapHit(Order order) {
    BazaarNotifier.config.bankModule.bazaarDailyAmount -= order.orderValue;
  }

  public static synchronized void reset() {
    BazaarNotifier.config.bankModule.bazaarProfit = 0;
    orderHistory.clear();
    rawDifference = 0;
  }
}