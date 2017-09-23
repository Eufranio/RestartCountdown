package restartcountdown;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

@Plugin(
        id = "restartcountdown",
        name = "RestartCountdown",
        description = "Simple plugin that adds an countdown to restart",
        authors = {
                "Eufranio"
        }
)
public class RestartCountdown {

    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private ConfigurationNode rootNode;
    private Thread thread;

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path configFile;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        this.logger.info("RestartCountdown is starting!");
        RestartCommand cmd = new RestartCommand();
        cmd.register();
        loadConfig();
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        loader = null;
        rootNode = null;
        if (thread != null) {
            thread = null;
        }
        loadConfig();
    }

    public class RestartCommand {
        public void register() {
            CommandSpec spec = CommandSpec.builder()
                    .description(Text.of("Starts an countdown, or stops if there's one runing"))
                    .permission("restartcountdown.admin")
                    .executor(new CommandExecutor() {
                        @Override
                        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                            src.sendMessage(Text.of("Starting the countdown!"));
                            if (thread == null) {
                                startCountdown();
                            } else {
                                stopCountdown();
                            }
                            return CommandResult.success();
                        }
                    })
                    .build();
            Sponge.getCommandManager().register(this, spec, "restartcountdown", "rc");
        }
    }

    public class RestartThread extends Thread {
        @Override
        public void run() {
            for (int time = rootNode.getNode("core", "time").getInt(); time >= 0; time--) {
                if (time == 0) {
                    try {
                        List<String> commands = rootNode.getNode("core", "messages", "commands").getList(TypeToken.of(String.class));
                        commands.forEach(cmd -> Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmd));
                        this.interrupt();
                    } catch (ObjectMappingException e) {}
                }
                if (!rootNode.getNode("core", "messages", "override", time).isVirtual()) {
                    String msg = rootNode.getNode("core", "messages", "override", time).getString();
                    Sponge.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(msg)));
                    continue;
                }
                final int sec = time;
                try {
                    if (rootNode.getNode("core", "messages", "warnIn").getList(TypeToken.of(Integer.class)).contains(time)) {
                        Sponge.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(rootNode.getNode("core", "messages", "default").getString().replace("%seconds%", Integer.toString(sec)))));
                    }
                } catch (ObjectMappingException e) {}
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
        }
    }

    public void startCountdown() {
        this.thread = new RestartThread();
    }

    public void stopCountdown() {
        thread.interrupt();
        thread = null;
    }

    public void loadConfig() {
        if (!configFile.toFile().exists()) {
            try {
                Sponge.getAssetManager().getAsset(this, "RestartCountdown.conf").get().copyToFile(configFile);
            } catch (IOException | NoSuchElementException e) {
                return;
            }
        }
        loader = HoconConfigurationLoader.builder().setPath(configFile).build();
        try {
            rootNode = loader.load();
        } catch (IOException e) {
            logger.warn("Error loading config!");
            e.printStackTrace();
        }
    }

}
