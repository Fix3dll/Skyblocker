package de.hysky.skyblocker.skyblock.dwarven;

import de.hysky.skyblocker.SkyblockerMod;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.utils.Utils;
import de.hysky.skyblocker.utils.scheduler.Scheduler;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;

public class CrystalsHud {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final Identifier MAP_TEXTURE = new Identifier(SkyblockerMod.NAMESPACE, "textures/gui/crystals_map.png"); 
    private static final Identifier MAP_ICON = new Identifier("textures/map/map_icons.png");
    private static final String[] SMALL_LOCATIONS = { "Fairy Grotto", "King", "Corleone" };

    public static boolean visible = false;

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("skyblocker")
                .then(ClientCommandManager.literal("hud")
                        .then(ClientCommandManager.literal("crystals")
                                .executes(Scheduler.queueOpenScreenCommand(CrystalsHudConfigScreen::new))))));

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (!SkyblockerConfigManager.get().locations.dwarvenMines.crystalsHud.enabled
                    || CLIENT.player == null
                    || !visible) {
                return;
            }
            render(context, SkyblockerConfigManager.get().locations.dwarvenMines.crystalsHud.x,
                    SkyblockerConfigManager.get().locations.dwarvenMines.crystalsHud.y);
        });
    }

    protected static IntIntPair getDimForConfig() {
        return IntIntPair.of(62, 62);
    }


    /**
     * Renders the map to the players UI. renders the background image ({@link CrystalsHud#MAP_TEXTURE}) of the map then if enabled special locations on the map. then finally the player to the map.
     *
     * @param context DrawContext to draw map to
     * @param hudX Top left X coordinate of the map
     * @param hudY Top left Y coordinate of the map
     */
    protected static void render(DrawContext context, int hudX, int hudY) {
        //draw map texture
        context.drawTexture(MAP_TEXTURE, hudX, hudY, 0, 0, 62, 62, 62, 62);

        //if enabled add waypoint locations to map
        if (SkyblockerConfigManager.get().locations.dwarvenMines.crystalsHud.showLocations) {
            Map<String,CrystalsWaypoint> ActiveWaypoints=  CrystalsLocationsManager.activeWaypoints;

            for (CrystalsWaypoint waypoint : ActiveWaypoints.values()) {
                Color waypointColor = waypoint.category.color;
                Pair<Integer, Integer> renderPos  = transformLocation(waypoint.pos.getX(),waypoint.pos.getZ());
                int locationSize  = SkyblockerConfigManager.get().locations.dwarvenMines.crystalsHud.locationSize;

                if (Arrays.asList(SMALL_LOCATIONS).contains(waypoint.name.getString())) {//if small location half the location size
                    locationSize = locationSize / 2;
                }

                //fill square of size locationSize around the coordinates of the location
                context.fill(hudX + renderPos.first() - locationSize / 2, hudY + renderPos.second() - locationSize / 2, hudX + renderPos.first() + locationSize / 2, hudY + renderPos.second() + locationSize / 2, waypointColor.getRGB());
            }
        }

        //draw player on map
        if (CLIENT.player == null || CLIENT.getNetworkHandler() == null) {
            return;
        }

        //get player location
        double playerX = CLIENT.player.getX();
        double playerZ = CLIENT.player.getZ();
        Pair<Integer, Integer> renderPos  = transformLocation(playerX,playerZ);
        //draw marker on map
        context.drawTexture(MAP_ICON, hudX + renderPos.first() - 2, hudY + renderPos.second() - 2, 58, 2, 4, 4, 128, 128);

        //todo add direction and scale (can not work out how to rotate)
    }

    /**
     * Converts an X and Z coordinate in the crystal hollow to a X and Y coordinate on the map.
     *
     * @param x the world X coordinate
     * @param z the world Z coordinate
     * @return the pair of values for x and y
     */
    protected static Pair<Integer, Integer> transformLocation(double x, double z) {
        //converts an x and z to a location on the map
        int transformedX = (int)((x - 202) / 621 * 62);
        int transformedY = (int)((z - 202) / 621 * 62);
        transformedX = MathHelper.clamp(transformedX, 0, 62);
        transformedY = MathHelper.clamp(transformedY, 0, 62);

        return Pair.of(transformedX,transformedY);
    }

    /**
     * Works out if the crystals map should be rendered and sets {@link CrystalsHud#visible} accordingly.
     *
     */
    public static void update() {
        if (CLIENT.player == null || CLIENT.getNetworkHandler() == null || !SkyblockerConfigManager.get().locations.dwarvenMines.crystalsHud.enabled) {
            visible = false;
            return;
        }

        //get if the player is in the crystals
        visible = Utils.isInCrystalHollows();
    }
}
