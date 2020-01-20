package me.Jeremaster101.Volleyball.Ball;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.Jeremaster101.Volleyball.Court.Court;
import me.Jeremaster101.Volleyball.Court.CourtHandler;
import me.Jeremaster101.Volleyball.Volleyball;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.UUID;

public class Ball {
    
    private ArmorStand stand;
    private Slime slime;
    private boolean end = false;
    private int task;
    private Player player;
    
    /**
     * Creates a new ball
     *
     * @param player player to create the ball at
     */
    public Ball(Player player) {
        this.player = player;
        
        CourtHandler ch = new CourtHandler();
    
        Court court = Court.getCourt(player, ch.getCourt(player));
        
        slime = player.getLocation().getWorld()
                .spawn(player.getEyeLocation().add(player.getLocation().getDirection()).subtract(0, 0.25, 0), Slime.class);
        slime.setSize(1);
        Location loc = slime.getLocation();
        loc.setYaw(0);
        loc.setPitch(0);
        slime.teleport(loc);
        
        stand = slime.getWorld().spawn(slime.getLocation().subtract(0, 1.5, 0), ArmorStand.class);
        stand.setVisible(false);
        setTexture(court.getTexture());
        stand.setGravity(false);
        stand.setCustomName("BALLSTAND");
        stand.setCustomNameVisible(false);
        stand.setSilent(true);
        
        slime.setCollidable(true);
        slime.setCustomName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "BALL");
        slime.setVelocity(player.getLocation().getDirection().multiply(0.1).add(new Vector(0, 1, 0)));
        slime.getWorld().playSound(slime.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2, 0);
        slime.setCustomNameVisible(false);
        slime.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 1, true, false));
        slime.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100000, 255, true, false));
        slime.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100000, 255, true, false));
        slime.setSilent(true);
    }
    
    /**
     * @param url link to the player skin to get the skull from
     */
    public void setTexture(String url) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        if (url.isEmpty())
            return;
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
        Field profileField = null;
        try {
            profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
            e1.printStackTrace();
        }
        head.setItemMeta(headMeta);
        stand.setHelmet(head);
    }
    
    /**
     * serves the volleyball
     */
    @SuppressWarnings("deprecation")
    public void serve(boolean animated) {
        if (animated) {
            Location loc = player.getLocation();
            double radius = 0.5;
            for (double y = 0; y <= 10; y += 0.2) {
                double finalY = y;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double x = (radius + 0.05 * finalY) * Math.cos(finalY);
                        double z = (radius + 0.05 * finalY) * Math.sin(finalY);
                        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
                                (float) (loc.getX() + x), (float) (loc.getY() + finalY * 0.1),
                                (float) (loc.getZ() + z), 0, 0, 0, 0, 1);
                    }
                }.runTaskLater(Volleyball.getInstance(), (long) y);
            }
        }
        
        task = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(Volleyball.getInstance(), new BukkitRunnable() {
            @Override
            public void run() {
                slime.setFallDistance(0);
                slime.getWorld().spawnParticle(Particle.END_ROD, slime.getLocation(), 0, 0, 0, 0, 1);
                slime.setTarget(null);
                
                if (slime.isDead()) {
                    stand.remove();
                    this.cancel();
                }
                stand.setFallDistance(0);
                if (!end) {
                    Location loc = slime.getLocation();
                    loc.setPitch(0);
                    loc.setYaw(0);
                    slime.teleport(loc);
                    stand.teleport(slime.getLocation().subtract(0, 1.5, 0));
                }
                
                if (slime.isOnGround() || slime.getLocation().getBlock().getType() != Material.AIR) {
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (slime.isOnGround() || slime.getLocation().add(0, 0.5, 0).getBlock().getType() != Material.AIR) {
                                remove(true);
                            }
                        }
                    }.runTaskLater(Volleyball.getInstance(), 5);
                }
            }
        }, 0, 1);
    }
    
    /**
     * removes the volleyball with or without animations
     *
     * @param animated whether to animate the removal or not
     */
    public void remove(boolean animated) {
        if (animated) {
            end = true;
            double radius = 1;
            Location loc = slime.getLocation();
            for (double y = 0; y <= 6.28; y += 0.2) {
                double finalY = y;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double x = (radius - 0.14 * finalY) * Math.cos(finalY);
                        double z = (radius - 0.14 * finalY) * Math.sin(finalY);
                        slime.getWorld().spawnParticle(Particle.CRIT_MAGIC,
                                (float) (loc.getX() + x), (float) (loc.getY() + 0.2),
                                (float) (loc.getZ() + z), 0, 0, 0, 0, 1);
                        Location loc1 = stand.getLocation();
                        loc1.setYaw((float) finalY * 20);
                        loc1.setY(loc1.subtract(0, 0.1 * finalY, 0).getY());
                        stand.teleport(loc1);
                    }
                }.runTaskLater(Volleyball.getInstance(), (long) y);
            }
            for (double y = 0; y <= 6.28; y += 0.2) {
                double finalY = y;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        double x = (radius - 0.14 * finalY) * Math.cos(finalY + 3.14159);
                        double z = (radius - 0.14 * finalY) * Math.sin(finalY + 3.14159);
                        slime.getWorld().spawnParticle(Particle.CRIT_MAGIC,
                                (float) (loc.getX() + x), (float) (loc.getY() + 0.2),
                                (float) (loc.getZ() + z), 0, 0, 0, 0, 1);
                    }
                }.runTaskLater(Volleyball.getInstance(), (long) y);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    slime.remove();
                    stand.remove();
                }
            }.runTaskLater(Volleyball.getInstance(), (long) 6.28);
            
            slime.getWorld().playSound(slime.getLocation(), Sound.BLOCK_SAND_PLACE, 2, 1);
            slime.getWorld().playSound(slime.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 2, 1);
            Bukkit.getServer().getScheduler().cancelTask(task);
        } else {
            Bukkit.getServer().getScheduler().cancelTask(task);
            slime.remove();
            stand.remove();
        }
    }
}