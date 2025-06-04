package dev.meyi.bn.handlers;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.platform.Platform;
import dev.meyi.bn.BazaarNotifier;
import dev.meyi.bn.json.Order;
import dev.meyi.bn.modules.calc.BankCalculator;
import dev.meyi.bn.utilities.ChatMessageParser;
import dev.meyi.bn.utilities.GuiStringUtils;
import dev.meyi.bn.utilities.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

@SuppressWarnings("unused")
public class EventHandler {

  static Order verify = null;

  @SubscribeEvent
  public void bazaarChatHandler(ClientChatReceivedEvent e) {

    if (!BazaarNotifier.activeBazaar) {
      return;
    }
    String message = StringUtils.stripControlCodes(e.message.getUnformattedText());

    Order order = null;

    order = ChatMessageParser.parseTransaction(message);
    if (order != null) {
      BankCalculator.evaluate(message); // This still takes a message, not an Order.
      return;
    }

    order = ChatMessageParser.parseOrderSetup(message);
    if (order != null) {
      handleSetupOrder(order);
      return;
    }

    order = ChatMessageParser.parseOrderFilled(message);
    if (order != null) {
      handleOrderFilled(order);
      return;
    }

    order = ChatMessageParser.parseCancellation(message);
    if (order != null) {
      handleOrderCancellation(order);
      return;
    }
    
    if (message.startsWith("Bazaar! Claimed ") || message.startsWith("[Bazaar] Claimed")) {
      ChestTickHandler.lastScreenDisplayName = "";
    }
  }

  @SubscribeEvent
  public void menuOpenedEvent(GuiOpenEvent e) {
    if (e.gui instanceof GuiChest) {
      IInventory chest = ReflectionHelper.getLowerChestInventory((GuiChest) e.gui);
      if (chest != null && chest.hasCustomName()) {
        String displayName = chest.getDisplayName().getUnformattedText();
        if (GuiStringUtils.isBazaarGui(displayName) || BazaarNotifier.forceRender) {
          BazaarNotifier.inBazaar = true;
        }
      }
    } else if (e.gui == null || e.gui instanceof GuiEditSign) {
      BazaarNotifier.inBazaar = false;
    }
  }

  @SubscribeEvent
  public void disconnectEvent(ClientDisconnectionFromServerEvent e) {
    BazaarNotifier.inBazaar = false;
  }

  @SubscribeEvent
  public void renderEvent(TickEvent e) {
    if (BazaarNotifier.guiToOpen.contains("settings")) {
      if (Platform.getGuiPlatform().getCurrentScreen() == null) {
        Minecraft.getMinecraft().displayGuiScreen(OneConfigGui.create());
        BazaarNotifier.guiToOpen = "settings-mod";
      } else if (BazaarNotifier.guiToOpen.equals("settings-mod")
          && Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui) {
        OneConfigGui.INSTANCE.openPage(
            new ModConfigPage(BazaarNotifier.config.mod.defaultPage, true),
            new cc.polyfrost.oneconfig.gui.animations.DummyAnimation(2128), false);
        BazaarNotifier.guiToOpen = "";
      }
    }
  }

  private void handleSetupOrder(Order orderInfo) {
    if (verify != null) {
    } else {
    }

    if (verify != null && verify.matches(orderInfo)) {
      BazaarNotifier.orders.add(verify);
      BankCalculator.evaluateCapHit(verify);
      verify = null;
    } else {
    }
  }
  
  private void handleOrderFilled(Order orderInfo) {
    int orderToRemove = findBestMatchingOrder(orderInfo.productID, orderInfo.startAmount, orderInfo.type);
    
    if (orderToRemove != -1) {
      BazaarNotifier.orders.remove(orderToRemove);
    } else {
    }
  }
  
  private int findBestMatchingOrder(String itemName, int amount, Order.OrderType type) {
    int bestIndex = -1;
    double edgePrice = (type == Order.OrderType.BUY) ? Double.MIN_VALUE : Double.MAX_VALUE;
    
    for (int i = 0; i < BazaarNotifier.orders.size(); i++) {
      Order order = BazaarNotifier.orders.get(i);
      
      if (order.productID.equalsIgnoreCase(itemName) && 
          order.startAmount == amount && 
          order.type.equals(type)) {
        
        boolean isBetterMatch = (type == Order.OrderType.BUY && order.pricePerUnit > edgePrice) ||
                               (type == Order.OrderType.SELL && order.pricePerUnit < edgePrice);
        
        if (isBetterMatch) {
          edgePrice = order.pricePerUnit;
          bestIndex = i;
        }
      }
    }
    
    return bestIndex;
  }
  
  private void handleOrderCancellation(Order cancellation) {
    for (int i = 0; i < BazaarNotifier.orders.size(); i++) {
      Order order = BazaarNotifier.orders.get(i);
      
      if (cancellation.type == Order.OrderType.BUY && order.type.equals(Order.OrderType.BUY)) {
        // The refundCoins from cancellation is now in cancellation.pricePerUnit
        if (isMatchingBuyOrderCancellation(order, cancellation.pricePerUnit)) {
          BazaarNotifier.orders.remove(i);
          break;
        }
      } else if (cancellation.type == Order.OrderType.SELL && order.type.equals(Order.OrderType.SELL)) {
        // The refundAmount from cancellation is now in cancellation.startAmount
        if (isMatchingSellOrderCancellation(order, cancellation.startAmount, cancellation.productID)) {
          BazaarNotifier.orders.remove(i);
          break;
        }
      }
    }
  }
  
  private boolean isMatchingBuyOrderCancellation(Order order, double refundCoins) {
    double orderValue = (refundCoins >= 10000.0) ?
                       Math.round(order.orderValue) : order.orderValue;
    double remaining = orderValue - refundCoins;
    return remaining <= 1 && remaining >= 0;
  }
  
  private boolean isMatchingSellOrderCancellation(Order order, int refundAmount, String itemName) {
    // Convert order.product (display name) to product ID for comparison with itemName (product ID)
    String orderProductId = order.productID;
    return orderProductId.equalsIgnoreCase(itemName) && order.amountRemaining == refundAmount;
  }
}
