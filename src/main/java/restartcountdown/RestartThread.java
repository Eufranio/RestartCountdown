package restartcountdown;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.List;

/**
 * Created by Frani on 23/09/2017.
 */
public class RestartThread extends Thread {
    @Override
    public void run() {
        for (int time = RestartCountdown.rootNode.getNode("core", "time").getInt(); time >= 0; time--) {
            if (time == 0) {
                try {
                    List<String> commands = RestartCountdown.rootNode.getNode("core", "messages", "commands").getList(TypeToken.of(String.class));
                    commands.forEach(cmd -> Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmd));
                    this.interrupt();
                } catch (ObjectMappingException e) {}
            }
            if (!RestartCountdown.rootNode.getNode("core", "messages", "override", time).isVirtual()) {
                String msg = RestartCountdown.rootNode.getNode("core", "messages", "override", time).getString();
                Sponge.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(msg)));
                continue;
            }
            final int sec = time;
            try {
                if (RestartCountdown.rootNode.getNode("core", "messages", "warnIn").getList(TypeToken.of(Integer.class)).contains(time)) {
                    Sponge.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(RestartCountdown.rootNode.getNode("core", "messages", "default").getString().replace("%seconds%", Integer.toString(sec)))));
                }
            } catch (ObjectMappingException e) {}
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }
}