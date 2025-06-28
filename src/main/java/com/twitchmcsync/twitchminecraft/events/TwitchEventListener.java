package com.twitchmcsync.twitchminecraft.events;

import com.twitchmcsync.twitchminecraft.authentication.TwitchPlayer;
import com.twitchmcsync.twitchminecraft.reward.SyncReward;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class TwitchEventListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTwitchSync(TwitchSyncEvent event) {
        SyncReward allReward = SyncReward.getRewardMap().get("all");
        SyncReward noSub = SyncReward.getRewardMap().get("no_sub");
        SyncReward tierReward = SyncReward.getRewardMap().get("tier_" + event.getTier());

        TwitchPlayer player = event.getTwitchPlayer();
        if(event.getTier() == 0 && noSub != null) {
            noSub.executeSubscribeCommands(player);
            player.setSubbed(false);
            player.save();
            return;
        } else if(player.getTier() != 0) {
            allReward.executeSubscribeCommands(player);
            tierReward.executeSubscribeCommands(player);
        }

        //Set and Update their TwitchPlayer with the new subbed status.
        player.setSubbed(true);
        player.save();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTwitchExpire(TwitchUnsyncEvent event) {
        TwitchPlayer player = event.getTwitchPlayer();

        SyncReward allReward = SyncReward.getRewardMap().get("all");
        SyncReward noSubReward = SyncReward.getRewardMap().get("no_sub");
        SyncReward tierReward = SyncReward.getRewardMap().get("tier_" + player.getTier());

        if(player.getTier() == 0 && noSubReward != null) {
            noSubReward.executeExpireCommands(player.getPlayer().getName(), player);
        } else if (player.getTier() != 0){
            allReward.executeExpireCommands(player.getPlayer().getName(), player);
            tierReward.executeExpireCommands(player.getPlayer().getName(), player);
        }

        //Set and Update their TwitchPlayer with the new subbed status.
        if(event.getMethod() == TwitchUnsyncEvent.ExpireMethod.NOT_SUBBED) {
            player.setSubbed(false);
            player.save();
        }
    }


}
