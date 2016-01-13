/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
