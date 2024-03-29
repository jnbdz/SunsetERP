import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import java.util.Collections;
import java.util.Iterator;

public class OfbizXmlConfigSourceProvider implements ConfigSourceProvider {
    private final String xmlFilePath;

    public OfbizXmlConfigSourceProvider(String xmlFilePath) {
        this.xmlFilePath = xmlFilePath;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return Collections.singleton(new OfbizXmlConfigSource(xmlFilePath));
    }
}