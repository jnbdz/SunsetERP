package org.sitenetsoft.sunseterp.framework.entity.transaction;

import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.SystemException;

public class JakartaToJavaxTransactionManagerAdapter implements javax.transaction.TransactionManager {
    private final jakarta.transaction.TransactionManager delegate;

    public JakartaToJavaxTransactionManagerAdapter(jakarta.transaction.TransactionManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void begin() throws javax.transaction.NotSupportedException, javax.transaction.SystemException {
        try {
            delegate.begin();
        } catch (jakarta.transaction.NotSupportedException e) {
            throw new javax.transaction.NotSupportedException(e.getMessage());
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void commit() throws javax.transaction.RollbackException, javax.transaction.HeuristicMixedException,
            javax.transaction.HeuristicRollbackException, javax.transaction.SystemException {
        try {
            delegate.commit();
        } catch (jakarta.transaction.RollbackException e) {
            throw new javax.transaction.RollbackException(e.getMessage());
        } catch (jakarta.transaction.HeuristicMixedException e) {
            throw new javax.transaction.HeuristicMixedException(e.getMessage());
        } catch (jakarta.transaction.HeuristicRollbackException e) {
            throw new javax.transaction.HeuristicRollbackException(e.getMessage());
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void rollback() throws javax.transaction.SystemException {
        try {
            delegate.rollback();
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void setRollbackOnly() throws javax.transaction.SystemException {
        try {
            delegate.setRollbackOnly();
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public int getStatus() throws javax.transaction.SystemException {
        try {
            return delegate.getStatus();
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public javax.transaction.Transaction getTransaction() throws javax.transaction.SystemException {
        try {
            jakarta.transaction.Transaction txn = delegate.getTransaction();
            return txn == null ? null : new JakartaToJavaxTransactionAdapter(txn);
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void setTransactionTimeout(int seconds) throws javax.transaction.SystemException {
        try {
            delegate.setTransactionTimeout(seconds);
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public javax.transaction.Transaction suspend() throws javax.transaction.SystemException {
        try {
            jakarta.transaction.Transaction txn = delegate.suspend();
            return txn == null ? null : new JakartaToJavaxTransactionAdapter(txn);
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void resume(javax.transaction.Transaction tobj) throws javax.transaction.SystemException {
        try {
            delegate.resume(((JakartaToJavaxTransactionAdapter) tobj).getDelegate());
        } catch (InvalidTransactionException | SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }
}

class JakartaToJavaxTransactionAdapter implements javax.transaction.Transaction {
    private final jakarta.transaction.Transaction delegate;

    public JakartaToJavaxTransactionAdapter(jakarta.transaction.Transaction delegate) {
        this.delegate = delegate;
    }

    public jakarta.transaction.Transaction getDelegate() {
        return delegate;
    }

    @Override
    public void commit() throws javax.transaction.RollbackException, javax.transaction.HeuristicMixedException,
            javax.transaction.HeuristicRollbackException, javax.transaction.SystemException {
        try {
            delegate.commit();
        } catch (jakarta.transaction.RollbackException e) {
            throw new javax.transaction.RollbackException(e.getMessage());
        } catch (jakarta.transaction.HeuristicMixedException e) {
            throw new javax.transaction.HeuristicMixedException(e.getMessage());
        } catch (jakarta.transaction.HeuristicRollbackException e) {
            throw new javax.transaction.HeuristicRollbackException(e.getMessage());
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void rollback() throws javax.transaction.SystemException {
        try {
            delegate.rollback();
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void setRollbackOnly() throws javax.transaction.SystemException {
        try {
            delegate.setRollbackOnly();
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public int getStatus() throws javax.transaction.SystemException {
        try {
            return delegate.getStatus();
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public boolean enlistResource(javax.transaction.xa.XAResource xaRes) throws javax.transaction.RollbackException,
            javax.transaction.SystemException {
        try {
            return delegate.enlistResource(xaRes);
        } catch (jakarta.transaction.RollbackException e) {
            throw new javax.transaction.RollbackException(e.getMessage());
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public boolean delistResource(javax.transaction.xa.XAResource xaRes, int flag) throws javax.transaction.SystemException {
        try {
            return delegate.delistResource(xaRes, flag);
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }

    @Override
    public void registerSynchronization(javax.transaction.Synchronization sync) throws javax.transaction.RollbackException,
            javax.transaction.SystemException {
        try {
            delegate.registerSynchronization(new jakarta.transaction.Synchronization() {
                @Override
                public void beforeCompletion() {
                    sync.beforeCompletion();
                }

                @Override
                public void afterCompletion(int status) {
                    sync.afterCompletion(status);
                }
            });
        } catch (jakarta.transaction.RollbackException e) {
            throw new javax.transaction.RollbackException(e.getMessage());
        } catch (jakarta.transaction.SystemException e) {
            throw new javax.transaction.SystemException(e.getMessage());
        }
    }
}
