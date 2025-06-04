package dev.meyi.bn.utilities;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.meyi.bn.BazaarNotifier;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Utility class for managing version checking and update notifications.
 */
public final class VersionManager {
    
    private static final Pattern BETA_VERSION_PATTERN = Pattern.compile(
        "\"?([0-9]+\\.[0-9]+\\.[0-9]+)-beta([0-9]+)\"?$", Pattern.MULTILINE);
    
    private static final String GITHUB_API_LATEST_RELEASE = 
        "https://api.github.com/repos/symt/BazaarNotifier/releases/latest";
    
    private static final String GITHUB_BUILD_GRADLE_BETA = 
        "https://raw.githubusercontent.com/symt/BazaarNotifier/beta/build.gradle.kts";
    
    private static final String UPDATE_LINK = 
        "https://github.com/symt/BazaarNotifier/releases/latest";

    public static class VersionInfo {
        public final String[] versionParts;
        public final int betaNumber;
        public final boolean isBeta;
        
        public VersionInfo(String version) {
            Matcher matcher = BETA_VERSION_PATTERN.matcher(version);
            if (matcher.find()) {
                this.isBeta = true;
                this.versionParts = matcher.group(1).split("\\.");
                this.betaNumber = Integer.parseInt(matcher.group(2));
            } else {
                this.isBeta = false;
                this.versionParts = version.split("\\.");
                this.betaNumber = 0;
            }
        }
        
        public VersionInfo(String[] versionParts, int betaNumber, boolean isBeta) {
            this.versionParts = versionParts;
            this.betaNumber = betaNumber;
            this.isBeta = isBeta;
        }
    }

    public enum VersionStatus {
        UP_TO_DATE,
        OUTDATED,
        UNRELEASED,
        BETA_OUTDATED,
        BETA_UNRELEASED,
        BETA_CURRENT
    }

    public static void checkForUpdates() {
        try {
            VersionInfo currentVersion = new VersionInfo(BazaarNotifier.VERSION);
            VersionInfo latestVersion = getLatestVersion(currentVersion.isBeta);
            
            VersionStatus status = compareVersions(currentVersion, latestVersion);
            sendUpdateMessage(status);
            
        } catch (IOException e) {
            sendErrorMessage("There was an error checking for updates: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static VersionInfo getLatestVersion(boolean checkBeta) throws IOException {
        if (checkBeta) {
            return getLatestBetaVersion();
        } else {
            return getLatestReleaseVersion();
        }
    }

    private static VersionInfo getLatestBetaVersion() throws IOException {
        String buildGradle = IOUtils.toString(new BufferedReader(new InputStreamReader(
            HttpClientBuilder.create().build().execute(new HttpGet(GITHUB_BUILD_GRADLE_BETA))
                .getEntity().getContent())));
        
        Matcher matcher = BETA_VERSION_PATTERN.matcher(buildGradle);
        if (matcher.find()) {
            String[] versionParts = matcher.group(1).split("\\.");
            int betaNumber = Integer.parseInt(matcher.group(2));
            return new VersionInfo(versionParts, betaNumber, true);
        } else {
            throw new IOException("No beta version found in build.gradle file");
        }
    }

    private static VersionInfo getLatestReleaseVersion() throws IOException {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(IOUtils.toString(new BufferedReader(new InputStreamReader(
            HttpClientBuilder.create().build().execute(new HttpGet(GITHUB_API_LATEST_RELEASE))
                .getEntity().getContent()))), JsonObject.class);
        
        String[] versionParts = json.get("tag_name").getAsString().split("\\.");
        return new VersionInfo(versionParts, 0, false);
    }

    private static VersionStatus compareVersions(VersionInfo current, VersionInfo latest) {
        int versionComparison = compareVersionNumbers(current.versionParts, latest.versionParts);
        
        if (versionComparison == 0) {
            if (current.isBeta && latest.isBeta) {
                if (current.betaNumber > latest.betaNumber) {
                    return VersionStatus.BETA_UNRELEASED;
                } else if (current.betaNumber < latest.betaNumber) {
                    return VersionStatus.BETA_OUTDATED;
                } else {
                    return VersionStatus.BETA_CURRENT;
                }
            } else if (!current.isBeta && !latest.isBeta) {
                return VersionStatus.UP_TO_DATE;
            }
        } else if (versionComparison > 0) {
            return VersionStatus.UNRELEASED;
        } else {
            return VersionStatus.OUTDATED;
        }
        
        return VersionStatus.UP_TO_DATE;
    }

    private static int compareVersionNumbers(String[] current, String[] latest) {
        if (current.length != 3 || latest.length != 3) {
            return 0; // Invalid version format
        }
        
        for (int i = 0; i < 3; i++) {
            int currentPart = Integer.parseInt(current[i]);
            int latestPart = Integer.parseInt(latest[i]);
            
            if (currentPart != latestPart) {
                return Integer.compare(currentPart, latestPart);
            }
        }
        
        return 0; // All parts are equal
    }

    private static void sendUpdateMessage(VersionStatus status) {
        switch (status) {
            case UP_TO_DATE:
                // No message for up-to-date versions
                break;
                
            case OUTDATED:
                sendOutdatedMessage();
                break;
                
            case UNRELEASED:
                sendUnreleasedMessage();
                break;
                
            case BETA_OUTDATED:
                sendBetaOutdatedMessage();
                break;
                
            case BETA_UNRELEASED:
                sendBetaUnreleasedMessage();
                break;
                
            case BETA_CURRENT:
                sendBetaCurrentMessage();
                break;
        }
    }

    private static void sendOutdatedMessage() {
        ChatComponentText updateLink = new ChatComponentText(
            EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.BOLD + "[UPDATE LINK]");
        updateLink.setChatStyle(updateLink.getChatStyle()
            .setChatClickEvent(new ClickEvent(Action.OPEN_URL, UPDATE_LINK)));
        
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
            BazaarNotifier.prefix + EnumChatFormatting.RED
                + "The mod version that you're on is outdated. Please update for the best profits: ")
            .appendSibling(updateLink));
    }

    private static void sendUnreleasedMessage() {
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(BazaarNotifier.prefix + EnumChatFormatting.RED
                + "This version hasn't been released yet. Please report any bugs that you come across."));
    }

    private static void sendBetaOutdatedMessage() {
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(BazaarNotifier.prefix + EnumChatFormatting.RED
                + "You are on an outdated beta version. Please update via the discord server. Run /bn discord for the link"));
    }

    private static void sendBetaUnreleasedMessage() {
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(BazaarNotifier.prefix + EnumChatFormatting.RED
                + "This beta version hasn't been released yet. Please report any bugs that you come across."));
    }

    private static void sendBetaCurrentMessage() {
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(BazaarNotifier.prefix + EnumChatFormatting.GREEN
                + "You are on a beta version. Please report any bugs you come across in the discord server."));
    }

    private static void sendErrorMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(BazaarNotifier.prefix + EnumChatFormatting.RED + message));
    }
}
