package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import thunder.hack.events.impl.EventDeath;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.utility.ThunderUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public final class AutoEZ extends Module {

    public static ArrayList<String> EZWORDS = new ArrayList<>();
    public Setting<Boolean> global = new Setting<>("global", true);

    private final Setting<ModeEn> mode = new Setting<>("Mode", ModeEn.Basic);
    private final Setting<ServerMode> server = new Setting<>("Server", ServerMode.Universal);

    private final String[] EZ = new String[]{
            "%player% АНБРЕЙН ГЕТАЙ ТХ РЕКОД",
            "%player% ТВОЯ МАТЬ БУДЕТ СЛЕДУЮЩЕЙ))))",
            "%player% БИЧАРА БЕЗ ТХ",
            "%player% ЧЕ ТАК БЫСТРО СЛИЛСЯ ТО А?",
            "%player% ПЛАЧЬ",
            "%player% УПССС ЗАБЫЛ КИЛЛКУ ВЫРУБИТЬ",
            "ОДНОКЛЕТОЧНЫЙ %player% БЫЛ ВПЕНЕН",
            "%player% ИЗИ БЛЯТЬ АХААХАХАХАХААХ",
            "%player% БОЖЕ МНЕ ТЕБЯ ЖАЛКО ВГЕТАЙ ТХ",
            "%player% ОПРАВДЫВАЙСЯ В ХУЙ ЧЕ СДОХ ТО)))",
            "%player% СПС ЗА ОТСОС)))"
    };

    public AutoEZ() {
        super("AutoEZ", Category.MISC);
        loadEZ();
    }

    public static void loadEZ() {
        try {
            File file = new File("ThunderHackRecode/misc/AutoEZ.txt");
            if (!file.exists()) file.createNewFile();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

                    ArrayList<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) lines.add(line);

                    EZWORDS.clear();
                    ArrayList<String> spamList = new ArrayList<>();

                    boolean multiline = lines.stream().anyMatch(String::isEmpty);

                    if (multiline) {
                        StringBuilder chunk = new StringBuilder();
                        for (String l : lines) {
                            if (l.isEmpty()) {
                                if (!chunk.isEmpty()) {
                                    spamList.add(chunk.toString());
                                    chunk.setLength(0);
                                }
                            } else chunk.append(l).append(" ");
                        }
                        if (!chunk.isEmpty()) spamList.add(chunk.toString());
                    } else spamList.addAll(lines);

                    EZWORDS = spamList;
                } catch (Exception ignored) {}
            }).start();
        } catch (IOException ignored) {}
    }

    @Override
    public void onEnable() {
        loadEZ();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;
        if (server.getValue() == ServerMode.Universal) return;

        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            if (packet.content().getString().contains("Вы убили игрока")) {
                String name = ThunderUtility.solveName(packet.content().getString());
                if (Objects.equals(name, "FATAL ERROR")) return;
                sayEZ(name);
            }
        }
    }

    @EventHandler
    public void onDeath(EventDeath e) {
        if (server.getValue() != ServerMode.Universal) return;

        if (Aura.target != null && Aura.target == e.getPlayer()) {
            sayEZ(e.getPlayer().getName().getString());
        }
    }

    private void sayEZ(String playerName) {
        String msg;

        if (mode.getValue() == ModeEn.Basic) {
            msg = EZ[new Random().nextInt(EZ.length)];
        } else {
            if (EZWORDS.isEmpty()) {
                sendMessage(isRu() ? "Файл AutoEZ пуст!" : "AutoEZ.txt is empty!");
                return;
            }
            msg = EZWORDS.get(new Random().nextInt(EZWORDS.size()));
        }

        msg = msg.replace("%player%", playerName);
        mc.player.networkHandler.sendChatMessage(global.getValue() ? "!" + msg : msg);
    }

    public enum ModeEn {
        Custom,
        Basic
    }

    public enum ServerMode {
        Universal,
        FunnyGame
    }
}
