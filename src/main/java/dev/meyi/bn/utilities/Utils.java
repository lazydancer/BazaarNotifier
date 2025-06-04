package dev.meyi.bn.utilities;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.meyi.bn.BazaarNotifier;
import dev.meyi.bn.json.resp.BazaarResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import org.apache.commons.lang3.StringUtils;

public class Utils {
  private static String playerUUID = "";

  public static BazaarResponse getBazaarData() {
    return HttpUtils.fetchBazaarData();
  }


  /**
   * Waiting until a proper backend is built out before completing this feature.
   *
   * @see <a
   * href="https://github.com/symt/BazaarNotifier/blob/02114fbef16786c69d7b560d76de53f643970f7e/src/main/java/dev/meyi/bn/utilities/Utils.java#L64">the
   * old code</a>
   */
  public static List<String> unlockedRecipes() {
    if (BazaarNotifier.config.collectionCheck) {
      if (playerUUID.equals("")) {
        String username = Minecraft.getMinecraft().getSession().getUsername();
        playerUUID = HttpUtils.fetchPlayerUuid(username);
        if (playerUUID == null) {
          return null;
        }
      }
    }
    return null;
  }

  public static void updateResources() throws ClassCastException {
    try {
      JsonObject resources = HttpUtils.fetchJsonWithTrustAll(BazaarNotifier.RESOURCE_LOCATION);

      if (resources != null) {
        BazaarNotifier.resources = resources;
        BazaarNotifier.bazaarConv = jsonToBimap(
            BazaarNotifier.resources.getAsJsonObject("bazaarConversions"));
        BazaarNotifier.enchantCraftingList = BazaarNotifier.resources
            .getAsJsonObject("enchantCraftingList");
      }
    } catch (ClassCastException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public static BiMap<String, String> jsonToBimap(JsonObject jsonObject) {
    BiMap<String, String> b = HashBiMap.create();
    Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
    for (Map.Entry<String, JsonElement> entry : entries) {
      try {
        b.put(entry.getKey(), jsonObject.get(entry.getKey()).getAsString());
      } catch (IllegalArgumentException ignored) {
      }
    }
    return b;
  }

  public static void saveResources(File file, JsonObject resources) {
    Gson gson = new Gson();
    try {
      if (!file.isFile()) {
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
      }
      Files.write(Paths.get(file.getAbsolutePath()),
          gson.toJson(resources).getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String[] getItemIdFromName(String userInput) {
    int threshold = userInput.length() / 3;
    String closestConversion = "";
    int minLevenshteinDistance = threshold + 1;

    for (String key : BazaarNotifier.bazaarConv.values()) {
      int levenshteinDistance = StringUtils
          .getLevenshteinDistance(userInput.toLowerCase(), key.toLowerCase(), threshold);
      if (levenshteinDistance != -1) {
        if (minLevenshteinDistance > levenshteinDistance) {
          minLevenshteinDistance = levenshteinDistance;
          closestConversion = key;
          if (levenshteinDistance == 0) {
            break;
          }
        }
      }
    }

    return new String[]{closestConversion,
        BazaarNotifier.bazaarConv.inverse().getOrDefault(closestConversion, "")};
  }

  public static List<String> getLoreFromItemStack(ItemStack item) {
    NBTTagList lorePreFilter = item.getTagCompound().getCompoundTag("display")
        .getTagList("Lore", 8);

    List<String> lore = new ArrayList<>();

    for (int j = 0; j < lorePreFilter.tagCount(); j++) {
      lore.add(net.minecraft.util.StringUtils.stripControlCodes(lorePreFilter.getStringTagAt(j)));
    }

    return lore;
  }

  public static int getOrderAmountLeft(List<String> lore, int totalAmount) {
    int amountLeft;
    if (lore.get(3).startsWith("Filled:")) {
      if (lore.get(3).split(" ")[2].contains("100%")) {
        amountLeft = 0;
      } else {
        String intToParse = lore.get(3).split(" ")[1].split("/")[0];
        int amountFulfilled;

        if (intToParse.contains("k")) {
          amountFulfilled = (int) (Double.parseDouble(intToParse.replace("k", "")) * 1000);
        } else {
          amountFulfilled = Integer.parseInt(intToParse);
        }

        amountLeft = totalAmount - amountFulfilled;
      }
    } else {
      amountLeft = totalAmount;
    }
    return amountLeft;
  }
}
