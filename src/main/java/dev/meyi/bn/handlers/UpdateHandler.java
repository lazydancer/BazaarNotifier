package dev.meyi.bn.handlers;

import dev.meyi.bn.utilities.VersionManager;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class UpdateHandler {

  private boolean firstJoin = true;

  @SubscribeEvent
  public void onPlayerJoinEvent(FMLNetworkEvent.ClientConnectedToServerEvent event) {
    if (firstJoin) {
      firstJoin = false;
      
      // Schedule version check after a delay to avoid interfering with login
      new ScheduledThreadPoolExecutor(1).schedule(
          VersionManager::checkForUpdates, 
          3,
          TimeUnit.SECONDS
      );
    }
  }
}
