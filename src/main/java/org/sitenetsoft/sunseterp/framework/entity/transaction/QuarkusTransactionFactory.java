package org.sitenetsoft.sunseterp.framework.entity.transaction;

// For JTA (Jakarta)
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

// For Quarkus injection
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// For your custom classes
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.config.model.Datasource;
import org.sitenetsoft.sunseterp.framework.entity.config.model.EntityConfig;
import org.sitenetsoft.sunseterp.framework.entity.datasource.GenericHelperInfo;
import org.sitenetsoft.sunseterp.framework.entity.jdbc.ConnectionFactoryLoader;

// For JDBC
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This Transaction Factory was for testing different solutions.
 * For the moment it is kept as reference.
 */
@ApplicationScoped
public class QuarkusTransactionFactory implements TransactionFactory {

    private static final String MODULE = QuarkusTransactionFactory.class.getName();

    @Inject
    TransactionManager tm;

    @Inject
    UserTransaction ut;

    @Override
    public TransactionManager getTransactionManager() {
        return tm;  // Quarkus Narayana
    }

    @Override
    public UserTransaction getUserTransaction() {
        return ut;  // Quarkus Narayana
    }

    @Override
    public String getTxMgrName() {
        return "quarkus-narayana";
    }

    @Override
    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        // Load <inline-jdbc> info from entityengine.xml
        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());
        if (datasourceInfo != null && datasourceInfo.getInlineJdbc() != null) {
            // DELEGATE to the existing DBCP-based loader
            return ConnectionFactoryLoader.getInstance().getConnection(helperInfo, datasourceInfo.getInlineJdbc());
        }
        Debug.logError("Quarkus is the configured transaction manager, but no inline-jdbc element was "
                        + "specified in the " + helperInfo.getHelperBaseName() +
                        " datasource. Please check your configuration",
                MODULE);
        return null;
    }

    @Override
    public void shutdown() {
        // If you want to explicitly close all DBCP pools (like GeronimoTransactionFactory did),
        // call ConnectionFactoryLoader.getInstance().closeAll()
        // Quarkus will handle Narayana's own shutdown, but doesn't automatically handle your custom pools.

        // Technically, Quarkus will shut itself down gracefully, but your DBCP pools are custom. If you want to ensure
        // no resources linger on certain app server stops
        ConnectionFactoryLoader.getInstance().closeAll();
    }
}
