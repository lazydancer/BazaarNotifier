package dev.meyi.bn.json;


import dev.meyi.bn.BazaarNotifier;
import dev.meyi.bn.json.resp.BazaarItem;
import dev.meyi.bn.utilities.RenderUtils;

import java.util.List;

public class Order {

  public String product;
  public int startAmount;
  public double pricePerUnit;
  public OrderStatus orderStatus = OrderStatus.SEARCHING;
  public double orderValue;
  public OrderType type;
  public long creationTime;
  private int amountRemaining;

  public Order(String product, OrderType type, double pricePerUnit, int startAmount) {
    this.product = product;
    this.type = type;
    this.pricePerUnit = pricePerUnit;
    this.startAmount = startAmount;

    amountRemaining = startAmount;
    orderValue = startAmount * pricePerUnit;
    creationTime = System.currentTimeMillis();
  }

  public int getAmountRemaining() {
    return amountRemaining;
  }

  public void setAmountRemaining(int amountRemaining) {
    this.amountRemaining = amountRemaining;
    orderValue = amountRemaining * pricePerUnit;
  }

  public boolean matches(Order other) {
    return other.type == this.type && other.product.equals(this.product)
        && other.startAmount == this.startAmount &&
        other.pricePerUnit == this.pricePerUnit;
  }

  public String getProductId() {
    return BazaarNotifier.bazaarConv.inverse().get(product);
  }

  public void updateStatus() {
    if (!BazaarNotifier.activeBazaar) return;

    List<BazaarItem.BazaarSubItem> summary =
            (type == OrderType.BUY)
                    ? BazaarNotifier.bazaarDataRaw.products.get(getProductId()).sell_summary
                    : BazaarNotifier.bazaarDataRaw.products.get(getProductId()).buy_summary;

    if (summary.isEmpty()
        || creationTime > BazaarNotifier.bazaarDataRaw.lastUpdated) {
      setStatus(OrderStatus.SEARCHING);
      return;
    }

    BazaarItem.BazaarSubItem best = summary.get(0);

    // factor = +1 for BUY  (higher API price means *we* are outdated if we're undercut),
    //          -1 for SELL (lower API price means *we* are outdated if we're overcut)
    int factor = (type == OrderType.BUY) ? 1 : -1;

    double diff = factor * (pricePerUnit - best.pricePerUnit);

    if (diff < 0) {
      setStatus(OrderStatus.OUTDATED);
    } else if (diff > 0) {
      setStatus(OrderStatus.SEARCHING);
    } else {
      // diff == 0: same price
      long samePriceCount = BazaarNotifier.orders.stream()
              .filter(o -> o.type == type
                      && o.getProductId().equals(getProductId())
                      && Double.compare(o.pricePerUnit, pricePerUnit) == 0)
              .count();

      if (best.orders == 1 && startAmount >= best.amount) {
        setStatus(OrderStatus.BEST);
      } else if (best.orders > 1 && samePriceCount != best.orders) {
        setStatus(OrderStatus.MATCHED);
      } else {
        setStatus(OrderStatus.SEARCHING);
      }
    }

  }

  private void setStatus(OrderStatus newStatus) {
    if (this.orderStatus == newStatus) return;

    switch (newStatus) {
      case BEST:
        if (this.orderStatus != OrderStatus.SEARCHING)
          RenderUtils.chatNotification(this, "REVIVED");
        break;
      case MATCHED:
        RenderUtils.chatNotification(this, "MATCHED");
        break;
      case OUTDATED:
        RenderUtils.chatNotification(this, "OUTDATED");
        break;
      default:
        break;
    }
    this.orderStatus = newStatus;
  }

  public enum OrderStatus {BEST, MATCHED, OUTDATED, SEARCHING}

  public enum OrderType {
    BUY("Buy Order"),
    SELL("Sell Offer");
    public final String longName;

    OrderType(String longName) {
      this.longName = longName;
    }
  }
}