package thunder.hack.features.modules.client;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.discord.DiscordEventHandlers;
import thunder.hack.utility.discord.DiscordRPC;
import thunder.hack.utility.discord.DiscordRichPresence;

import java.io.*;

public final class RPC extends Module {
    // Trỏ vào LazyLoader để tránh crash trên Mobile (Zaith/Pojav)
    private static final DiscordRPC rpc = DiscordRPC.LazyLoader.INSTANCE;

    public static Setting<Mode> mode = new Setting<>("Picture", Mode.Recode);
    public static Setting<Boolean> showIP = new Setting<>("ShowIP", true);
    public static Setting<sMode> smode = new Setting<>("StateMode", sMode.Stats);
    public static Setting<String> state = new Setting<>("State", "ThunderHack Recode NextGen");
    public static Setting<Boolean> nickname = new Setting<>("Nickname", true);
    
    public static DiscordRichPresence presence = new DiscordRichPresence();
    public static boolean started;
    private static Thread thread;
    static String String1 = "none";

    public RPC() {
        super("DiscordRPC", Category.CLIENT);
    }

    public enum Mode { Recode, Custom }
    public enum sMode { Stats, Custom, Version }

    // --- CÁC HÀM ĐỌC GHI FILE MÀ RPCCOMMAND CẦN ---
    public static void readFile() {
        try {
            File file = new File("ThunderHackRecode/misc/RPC.txt");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    while (reader.ready()) {
                        String1 = reader.readLine();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static void WriteFile(String url1, String url2) {
        File file = new File("ThunderHackRecode/misc/RPC.txt");
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(url1 + "SEPARATOR" + url2 + '\n');
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }
    // ----------------------------------------------

    @Override
    public void onDisable() {
        started = false;
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }
        if (rpc != null) {
            rpc.Discord_Shutdown();
        }
    }

    @Override
    public void onUpdate() {
        startRpc();
    }

    public void startRpc() {
        if (isDisabled() || rpc == null) return; 

        if (!started) {
            started = true;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1093053626198523935", handlers, true, "");
            presence.startTimestamp = (System.currentTimeMillis() / 1000L);
            presence.largeImageKey = "th_recode";
            presence.largeImageText = "v" + ThunderHack.VERSION;
            rpc.Discord_UpdatePresence(presence);

            thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    if (rpc == null) break;
                    rpc.Discord_RunCallbacks();
                    presence.details = getDetails();

                    switch (smode.getValue()) {
                        case Stats -> presence.state = "Modules: " + Managers.MODULE.getEnabledModules().size() + " / " + Managers.MODULE.modules.size();
                        case Custom -> presence.state = state.getValue();
                        case Version -> presence.state = "v" + ThunderHack.VERSION;
                    }

                    if (nickname.getValue() && mc.getSession() != null) {
                        presence.smallImageText = "Logged as " + mc.getSession().getUsername();
                        presence.smallImageKey = "https://minotar.net/helm/" + mc.getSession().getUsername() + "/100.png";
                    }
                    
                    rpc.Discord_UpdatePresence(presence);
                    try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
                }
            }, "Discord-RPC-Callback");
            thread.start();
        }
    }

    private String getDetails() {
        if (mc.currentScreen instanceof TitleScreen) return "In Main Menu";
        if (mc.currentScreen instanceof MultiplayerScreen || mc.currentScreen instanceof AddServerScreen) return "Selecting Server";
        if (mc.world == null) return "Loading...";
        if (mc.isInSingleplayer()) return "Singleplayer";
        if (showIP.getValue() && mc.getCurrentServerEntry() != null) return "Playing on " + mc.getCurrentServerEntry().address;
        return "Playing Multiplayer";
    }
}
