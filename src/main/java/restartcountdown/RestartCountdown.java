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
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
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
    public static ConfigurationNode rootNode;
    public static  Thread thread;
    private RestartCountdown instance;

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path configFile;

    @Listener
    public void onServerStart(GamePostInitializationEvent event) {
        this.logger.info("RestartCountdown is starting!");
        instance = this;
        RestartCommand cmd = new RestartCommand();
        cmd.register();
        loadConfig(instance);
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        loader = null;
        rootNode = null;
        if (thread != null) {
            thread = null;
        }
        loadConfig(instance);
    }

    public class RestartCommand {
        public void register() {
            CommandSpec spec = CommandSpec.builder()
                    .description(Text.of("Starts an countdown, or stops if there's one runing"))
                    .permission("restartcountdown.admin")
                    .executor(new CommandExecutor() {
                        @Override
                        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                            if (thread == null) {
                                src.sendMessage(Text.of("Starting the countdown!"));
                                startCountdown();
                            } else {
                                src.sendMessage(Text.of("Stopping the countdown!"));
                                stopCountdown();
                            }
                            return CommandResult.success();
                        }
                    })
                    .build();
            Sponge.getCommandManager().register(instance, spec, "restartcountdown", "rc");
        }
    }

    public void startCountdown() {
        thread = new RestartThread();
        thread.start();
    }

    public void stopCountdown() {
        thread.interrupt();
        thread = null;
    }

    public void loadConfig(RestartCountdown i) {
        if (!configFile.toFile().exists()) {
            try {
                Sponge.getAssetManager().getAsset(i, "RestartCountdown.conf").get().copyToFile(configFile);
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
