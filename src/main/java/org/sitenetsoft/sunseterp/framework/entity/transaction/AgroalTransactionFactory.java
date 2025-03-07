package org.sitenetsoft.sunseterp.framework.entity.transaction;

@ApplicationScoped
public class AgroalTransactionFactory implements TransactionFactory {

    @Inject
    jakarta.transaction.TransactionManager tm;

    @Inject
    jakarta.transaction.UserTransaction ut;

    // Quarkus's main DataSource
    @Inject
    javax.sql.DataSource dataSource;

    @Override
    public TransactionManager getTransactionManager() {
        return tm;
    }

    @Override
    public UserTransaction getUserTransaction() {
        return ut;
    }

    @Override
    public String getTxMgrName() {
        return "quarkus-narayana";
    }

    @Override
    public Connection getConnection(GenericHelperInfo helperInfo)
            throws SQLException, GenericEntityException {
        // Just ignore entityengine.xml's <inline-jdbc>, and always return
        // a Connection from Quarkus’s DataSource
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        // Quarkus handles shutting down Agroal automatically;
        // do nothing
    }
}

