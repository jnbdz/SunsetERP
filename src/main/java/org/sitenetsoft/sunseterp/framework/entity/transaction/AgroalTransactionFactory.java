package org.sitenetsoft.sunseterp.framework.entity.transaction;

// For JTA (Jakarta)
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

// For Quarkus injection
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// For your custom classes
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.datasource.GenericHelperInfo;

// For JDBC
import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p>AgroalTransactionFactory: a Quarkus-based TransactionFactory that uses Quarkus's
 * Narayana transaction manager and the Quarkus default (Agroal) data source.</p>
 *
 * <p>This replaces GeronimoTransactionFactory (or any other) from OFBiz
 * to integrate with Quarkus's JTA environment. It also replaces the
 * need for DBCPConnectionFactory, since Quarkus manages connection pooling via Agroal.</p>
 *
 * <p>Steps to enable this factory:</p>
 * <ul>
 *   <li>In <em>entityengine.xml</em>, set:
 *     <pre>{@code
 *     <transaction-factory class="org.sitenetsoft.sunseterp.framework.entity.transaction.AgroalTransactionFactory"/>
 *     }</pre>
 *   </li>
 *   <li>Configure your database via Quarkus properties in <em>application.properties</em>, e.g.:
 *     <pre>{@code
 *     quarkus.datasource.db-kind=postgresql
 *     quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydb
 *     quarkus.datasource.username=myuser
 *     quarkus.datasource.password=mypassword
 *     quarkus.datasource.jdbc.transactions=xa
 *     }</pre>
 *   </li>
 * </ul>
 */
@ApplicationScoped
public class AgroalTransactionFactory implements TransactionFactory {

    /**
     * The Quarkus-managed TransactionManager (Narayana).
     *
     * <p>Quarkus automatically injects a Jakarta TransactionManager instance,
     * letting OFBiz code call getTransactionManager() if needed.</p>
     */
    @Inject
    TransactionManager tm;

    /**
     * The Quarkus-managed UserTransaction (Narayana).
     *
     * <p>This is typically used if your code calls TransactionUtil.begin()/commit().</p>
     */
    @Inject
    UserTransaction ut;

    /**
     * The default Quarkus DataSource (Agroal pool), still under javax.sql.*.
     *
     * <p>Quarkus provides a "main" data source if you configure "quarkus.datasource.*" keys in
     * application.properties. This injection automatically picks it up.</p>
     *
     * <p>In Java 17 the SQL DataSource is still only found in Javax.</p>
     */
    @Inject
    javax.sql.DataSource dataSource;

    /**
     * Returns the Quarkus Narayana TransactionManager for integration with OFBiz TransactionUtil.
     *
     * @return the Quarkus (Narayana) TransactionManager
     */
    @Override
    public TransactionManager getTransactionManager() {
        return tm;
    }

    /**
     * Returns the Quarkus Narayana UserTransaction for integration with OFBiz TransactionUtil.
     *
     * @return the Quarkus (Narayana) UserTransaction
     */
    @Override
    public UserTransaction getUserTransaction() {
        return ut;
    }

    /**
     * Returns a short name to identify this TransactionManager in logs/config.
     *
     * @return "quarkus-narayana"
     */
    @Override
    public String getTxMgrName() {
        return "quarkus-narayana";
    }

    /**
     * Provides a JDBC {@link Connection} from the Quarkus Agroal DataSource.
     * <p>
     * <strong>Note:</strong> This ignores any <inline-jdbc> config in entityengine.xml,
     * because Quarkus manages the pool entirely via application.properties.</p>
     *
     * @param helperInfo the OFBiz helper info (ignored, since Quarkus config takes precedence)
     * @return a {@link Connection} from the Quarkus main DataSource
     * @throws SQLException           if obtaining the connection fails
     * @throws GenericEntityException if something else goes wrong at the entity-engine level
     */
    @Override
    public Connection getConnection(GenericHelperInfo helperInfo)
            throws SQLException, GenericEntityException {
        // Just return a connection from Agroal's Quarkus-managed DataSource.
        // Quarkus automatically enlists it in a JTA transaction if one is active.
        return dataSource.getConnection();
    }

    /**
     * Shuts down the Transaction Factory, but in Quarkus, this is effectively a no-op.
     * <p>
     * Quarkus automatically handles DataSource (Agroal) shutdown and the Narayana TM lifecycle.</p>
     */
    @Override
    public void shutdown() {
        // No manual shutdown needed; Quarkus manages its own lifecycle.
    }
}
