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
