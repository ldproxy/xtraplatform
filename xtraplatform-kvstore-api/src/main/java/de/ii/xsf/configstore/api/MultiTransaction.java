/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.configstore.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fischer
 */
public class MultiTransaction implements Transaction {

    protected final List<Transaction> transactions;

    public MultiTransaction() {
        this.transactions = new ArrayList<>();
    }
    
    public void addTransaction(Transaction transaction){
        transactions.add(transaction);
    }

    public void addTransactions(List<Transaction> transactions){
        this.transactions.addAll(transactions);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public void execute() throws IOException {
        for(Transaction t: transactions){
            t.execute();
        }
    }
    
    @Override
    public void commit() {
        for(Transaction t: transactions){
            t.commit();
        }
    }

    @Override
    public void rollback() {
        for(Transaction t: transactions){
            t.rollback();
        }
    }

    @Override
    public void close() {
        for(Transaction t: transactions){
            t.close();
        }
    }
    
}
