import io.quarkus.runtime.StartupEvent;
import javax.enterprise.event.Observes;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

@ApplicationScoped
public class ConfigImporter {

    void onStart(@Observes StartupEvent ev) {
        try {
            File inputFile = new File("path_to_your_OFBiz_XML_file");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("datasource");
            Node nNode = nList.item(0);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String dbKind = eElement.getAttribute("field-type-name");
                String username = eElement.getAttribute("jndi-username");
                String password = eElement.getAttribute("jndi-password");
                String jdbcUrl = eElement.getAttribute("jndi-jdbc");

                // Set the configuration properties in Quarkus
                System.setProperty("quarkus.datasource.db-kind", dbKind);
                System.setProperty("quarkus.datasource.username", username);
                System.setProperty("quarkus.datasource.password", password);
                System.setProperty("quarkus.datasource.jdbc.url", jdbcUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // You can add more specific error handling here if needed
        }
    }
}