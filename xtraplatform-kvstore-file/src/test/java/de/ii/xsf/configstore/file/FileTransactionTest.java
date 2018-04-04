/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xsf.configstore.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author fischer
 */
public class FileTransactionTest {
    
    public FileTransactionTest() {
    }
    
    private String tmpdir;
    private File file;
    private Writer writer;
    
    @BeforeClass(groups = {"default"})
    public void init() throws IOException {
        tmpdir = System.getProperty("java.io.tmpdir");
        file = new File(tmpdir+"/FileTransactionTest");
        
        file.createNewFile();
        
        writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
        
        writer.write("test");
        writer.close();
    }
    
    
    @Test(groups = {"default"})
    public void testCommit() throws IOException {
        file.createNewFile();
        File backupFile = new File(tmpdir+"/FileTransactionTest-backup");       
        
        DeleteFileTransaction ft = new DeleteFileTransaction(file);
        ft.execute();

        Assert.assertTrue(backupFile.exists());
        
        ft.commit();
        Assert.assertTrue(!backupFile.exists());
    }
    
    @Test(groups = {"default"})
    public void testRollback() throws IOException {
        file.createNewFile();
       
        File backupFile = new File(tmpdir+"/FileTransactionTest-backup");

        DeleteFileTransaction ft = new DeleteFileTransaction(file);
        ft.execute();

        Assert.assertTrue(backupFile.exists());
        
        ft.rollback();
        Assert.assertTrue(!backupFile.exists());
    }
    
    
    @AfterClass(groups = {"default"})
    public void cleanup() throws IOException {
        file.createNewFile();
        DeleteFileTransaction ft = new DeleteFileTransaction(file);
        
         File backupFile = new File(tmpdir+"/FileTransactionTest-backup");
        ft.execute();

        Assert.assertTrue(backupFile.exists());
        
        ft.commit();
        
        Assert.assertTrue(!backupFile.exists());
    }
}
