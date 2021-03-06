package world.bentobox.challenges.commands.admin;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import world.bentobox.challenges.ChallengesAddon;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Command to create a surrounding type challenge
 * @author tastybento
 * @deprecated Required blocks can be added via GUI. Not necessary.
 */
@Deprecated
public class CreateSurrounding extends CompositeCommand implements Listener {

    HashMap<UUID,SurroundChallengeBuilder> inProgress = new HashMap<>();

    /**
     * Admin command to make surrounding challenges
     * @param parent
     */
    public CreateSurrounding(Addon addon, CompositeCommand parent) {
        super(addon, parent, "surrounding");
        addon.getServer().getPluginManager().registerEvents(this, addon.getPlugin());
    }

    @Override
    public void setup() {
        this.setOnlyPlayer(true);
        this.setPermission("admin.challenges");
        this.setParametersHelp("challenges.commands.admin.surrounding.parameters");
        this.setDescription("challenges.commands.admin.surrounding.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            user.sendMessage("challenges.errors.no-name");
            return false;
        }
        // Tell user to hit objects to add to the surrounding object requirements
        user.sendMessage("challenges.messages.admin.hit-things");
        inProgress.put(user.getUniqueId(), new SurroundChallengeBuilder((ChallengesAddon) getAddon()).owner(user).name(args.get(0)));
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        e.setCancelled(inProgress.containsKey(e.getPlayer().getUniqueId()) ? true : false);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        inProgress.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public boolean onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) && inProgress.containsKey(e.getPlayer().getUniqueId())) {
            // Prevent damage
            e.setCancelled(true);
            inProgress.get(e.getPlayer().getUniqueId()).addBlock(e.getClickedBlock().getType());
            User.getInstance(e.getPlayer()).sendMessage("challenges.messages.admin.you-added", "[thing]", Util.prettifyText(e.getClickedBlock().getType().toString()));
            return true;
        }

        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return finished(e, e.getPlayer().getUniqueId());
        }
        return false;
    }

    private boolean finished(Cancellable e, UUID uuid) {
        if (inProgress.containsKey(uuid)) {
            e.setCancelled(true);
            boolean status = inProgress.get(uuid).build();
            if (status) {
                inProgress.get(uuid).getOwner().sendMessage("challenges.messages.admin.challenge-created", "[challenge]", inProgress.get(uuid).getName());
            }
            inProgress.remove(uuid);
            return status;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public boolean onPlayerInteract(PlayerInteractAtEntityEvent e) {
        return finished(e, e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public boolean onLeft(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            return false;
        }
        Player player = (Player)e.getDamager();
        if (inProgress.containsKey(player.getUniqueId())) {
            // Prevent damage
            e.setCancelled(true);
            inProgress.get(player.getUniqueId()).addEntity(e.getEntityType());
            User.getInstance(player).sendMessage("challenges.messages.admin.you-added", "[thing]", Util.prettifyText(e.getEntityType().toString()));
            return true;
        }
        return false;
    }


}
