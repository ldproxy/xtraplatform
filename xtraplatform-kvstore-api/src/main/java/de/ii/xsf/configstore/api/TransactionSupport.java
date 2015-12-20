package de.ii.xsf.configstore.api;

/**
 *
 * @author zahnen
 */
public interface TransactionSupport<T> {
    /**
     *
     * @param key the key
     * @return a new transaction
     */
    Transaction openDeleteTransaction(String key);

    /**
     *
     * @param key the key
     * @return a new transaction
     */
    WriteTransaction<T> openWriteTransaction(String key);
    
}
