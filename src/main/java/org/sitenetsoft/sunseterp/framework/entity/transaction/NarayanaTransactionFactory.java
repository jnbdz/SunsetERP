package org.sitenetsoft.sunseterp.framework.entity.transaction;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.config.model.Datasource;
import org.sitenetsoft.sunseterp.framework.entity.config.model.EntityConfig;
import org.sitenetsoft.sunseterp.framework.entity.datasource.GenericHelperInfo;
import org.sitenetsoft.sunseterp.framework.entity.jdbc.ConnectionFactoryLoader;

import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class NarayanaTransactionFactory implements TransactionFactory {

    private static final String MODULE = NarayanaTransactionFactory.class.getName();

    @Inject
    TransactionManager transactionManager;

    @Inject
    UserTransaction userTransaction;

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public UserTransaction getUserTransaction() {
        return userTransaction;
    }

    @Override
    public String getTxMgrName() {
        return "narayana";
    }

    @Override
    public Connection getConnection(GenericHelperInfo helperInfo) throws SQLException, GenericEntityException {
        Datasource datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());

        if (datasourceInfo != null && datasourceInfo.getInlineJdbc() != null) {
            return ConnectionFactoryLoader.getInstance().getConnection(helperInfo, datasourceInfo.getInlineJdbc());
        }
        Debug.logError("Narayana is the configured transaction manager but no inline-jdbc element was specified in the "
                + helperInfo.getHelperBaseName() + " datasource. Please check your configuration", MODULE);
        return null;
    }

    @Override
    public void shutdown() {
        ConnectionFactoryLoader.getInstance().closeAll();
    }
}