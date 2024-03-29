import io.smallrye.config.ConfigSource;
import io.smallrye.config.ConfigSourceWatcher;
import io.smallrye.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;



public class OfbizXmlConfigSource implements ConfigSource, ConfigSourceWatcher {
    private final Path xmlFilePath;
    private Map<String, ConfigValue> currentConfig;

    public OfbizXmlConfigSource(String xmlFilePath) {
        this.xmlFilePath = Paths.get(xmlFilePath);
        this.currentConfig = loadConfig();
    }

    @Override
    public Map<String, ConfigValue> getProperties() {
        return currentConfig;
    }

    @Override
    public int getOrdinal() {
        return 270; // Higher than application.properties (ordinal 260)
    }

    @Override
    public String getName() {
        return "OFBiz XML Config Source";
    }

    @Override
    public void onStartup() {
        // Watch for changes in the XML file
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            xmlFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            new Thread(() -> {
                while (true) {
                    WatchKey key;
                    try {
                        key = watchService.take();
                    } catch (InterruptedException e) {
                        return;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path changed = (Path) event.context();
                            if (changed.equals(xmlFilePath.getFileName())) {
                                // Reload the configuration
                                currentConfig = loadConfig();
                            }
                        }
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            }).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, ConfigValue> loadConfig() {
        // Load the configuration data from the OFBiz XML file
        // This will depend on the structure of your XML file
        Map<String, ConfigValue> config = new HashMap<>();

        // TODO: Read the XML file and populate the config map

        return config;
    }
}