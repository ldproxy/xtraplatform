/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraserver.framework.util.nativ;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple library class for working with JNI (Java Native Interface)
 *
 * @see http://frommyplayground.com/how-to-load-native-jni-library-from-jar
 *
 * @author Adam Heirnich <adam@adamh.cz>, http://www.adamh.cz
 */
public class NativeUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeUtils.class);
    private static final String NATIVE_DIR_PREFIX = "/nativ";

    private enum OS {

        WINDOWS_32("windows", "86", "32"),
        WINDOWS_64("windows", "64", "64"),
        UBUNTU_32("linux", "86", "32"),
        UBUNTU_64("linux", "64", "64"),
        NOTSUPPORTED("", "", "");
        public String name;
        private String bits;
        private String suffix;
        public String arch;

        private OS(String name, String arch, String bits) {
            this.name = name;
            this.bits = bits;
            //this.suffix = suffix;
            this.arch = arch;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getLibName(String lib) {
            if (this.name.equals("windows")) {
                return lib + ".dll";
            } else {
                return "lib" + lib + ".so";
            }
        }

        public String getLibPath(String lib) {
            return NATIVE_DIR_PREFIX + "/" + name.replace("linux", "ubuntu") + "/" + bits + "/" + getLibName(lib);
        }

        public String getLibPath() {
            return NATIVE_DIR_PREFIX + "/" + name + "/" + bits + "/";
        }

        public static OS fromString(String type, String arch) {
            for (OS v : OS.values()) {
                if (v != NOTSUPPORTED && type.contains(v.toString()) && arch.contains(v.arch)) {
                    return v;
                }
            }
            return NOTSUPPORTED;
        }

        public boolean isSupported() {
            return this != NOTSUPPORTED;
        }
    }

    /**
     * Private constructor - this class will never be instanced
     */
    private NativeUtils() {
    }

    /**
     * Loads library from current JAR archive
     *
     * The file from JAR is copied into system temporary directory and then
     * loaded. The temporary file is deleted after exiting. Method uses String
     * as filename because the pathname is "abstract", not system-dependent.
     *
     * @param filename The filename inside JAR as absolute path (beginning with
     * '/'), e.g. /package/File.ext
     * @throws IOException If temporary file creation or read/write operation
     * fails
     * @throws IllegalArgumentException If source file (param path) does not
     * exist
     * @throws IllegalArgumentException If the path is not absolute or if the
     * filename is shorter than three characters (restriction of {
     * @see File#createTempFile(java.lang.String, java.lang.String)}).
     */
    public static void loadLibraryFromJar(String path) throws IOException {

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("The path to be absolute (start with '/').");
        }

        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;

        // Split filename to prexif and suffix (extension)
        String prefix = "";
        String suffix = null;
        if (filename != null) {
            parts = filename.split("\\.", 2);
            prefix = parts[0];
            suffix = (parts.length > 1) ? "." + parts[parts.length - 1] : null; // Thanks, davs! :-)
        }

        // Check if the filename is okay
        if (filename == null || prefix.length() < 3) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }

        // Prepare temporary file
        File tempDir = Files.createTempDir();
        File temp = new File(tempDir, filename);
        temp.createNewFile();
        tempDir.deleteOnExit();
        temp.deleteOnExit();

        if (!temp.exists()) {
            throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
        }

        // Prepare buffer for data copying
        byte[] buffer = new byte[1024];
        int readBytes;

        // Open and check input stream
        InputStream is = NativeUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }

        // Open output stream and copy data between source file in JAR and the temporary file
        OutputStream os = new FileOutputStream(temp);
        try {
            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        } finally {
            // If read/write fails, close streams safely before throwing an exception
            os.close();
            is.close();
        }

        // Finally, load the library
        System.load(temp.getAbsolutePath());


        // workaround for temp dir cleanup
        /*final String libraryPrefix = prefix;
         final String lockSuffix = ".lock";
 
         // create lock file
         final File lock = new File( temp.getAbsolutePath() + lockSuffix);
         lock.createNewFile();
         lock.deleteOnExit();
 
         // file filter for library file (without .lock files)
         FileFilter tmpDirFilter =
         new FileFilter()
         {
         public boolean accept(File pathname)
         {
         return pathname.getName().startsWith( libraryPrefix) && !pathname.getName().endsWith( lockSuffix);
         }
         };
 
         // get all library files from temp folder  
         String tmpDirName = System.getProperty("java.io.tmpdir");
         File tmpDir = new File(tmpDirName);
         File[] tmpFiles = tmpDir.listFiles(tmpDirFilter);
 
         // delete all files which don't have n accompanying lock file
         for (int i = 0; i < tmpFiles.length; i++)
         {
         // Create a file to represent the lock and test.
         File lockFile = new File( tmpFiles[i].getAbsolutePath() + lockSuffix);
         if (!lockFile.exists())
         {
         tmpFiles[i].delete();
         }
         }*/
    }

    public static void loadLibrariesFromJar(String[] paths) throws IOException {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();

        OS os = getOs();
        LOGGER.info("DLL dir: {}", os.getLibPath());
        if (!os.isSupported()) {
            throw new IOException();
        }

        for (String name : paths) {

            // Prepare temporary file
            File temp = new File(tempDir, os.getLibName(name));
            temp.createNewFile();
            temp.deleteOnExit();

            if (!temp.exists()) {
                throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
            }

            // Prepare buffer for data copying
            byte[] buffer = new byte[1024];
            int readBytes;

            // Open and check input stream
            String path = os.getLibPath(name);
            InputStream is = NativeUtils.class.getResourceAsStream(path);
            if (is == null) {
                throw new FileNotFoundException("File " + path + " was not found inside JAR.");
            }

            // Open output stream and copy data between source file in JAR and the temporary file
            OutputStream ost = new FileOutputStream(temp);
            try {
                while ((readBytes = is.read(buffer)) != -1) {
                    ost.write(buffer, 0, readBytes);
                }
            } finally {
                // If read/write fails, close streams safely before throwing an exception
                ost.close();
                is.close();
            }

            
            // Finally, load the library
            System.load(temp.getAbsolutePath());
            

        }
    }

    /*public static String getNativeLibraryPath(String lib) {
     String dlldir = "";

     String osname = System.getProperty("os.name").toLowerCase();
     String osversion = System.getProperty("os.version");
     String arch = System.getProperty("os.arch");
        
     OS os = OS.fromString(osname);
     if (os.isSupported()) {
     if (arch.contains("64")) {
     dlldir = NATIVE_DIR_PREFIX + "/" + os.toString() + "/64/" + os.getLibName(lib);
     }
     else if (arch.contains("86")) {
     dlldir = NATIVE_DIR_PREFIX + "/" + os.toString() + "/32/" + os.getLibName(lib);
     }
     }
        

     return dlldir.replace("linux", "ubuntu");
     }*/
    public static OS getOs() {
        String osname = System.getProperty("os.name").toLowerCase();
        String osversion = System.getProperty("os.version");
        String arch = System.getProperty("os.arch");

        OS os = OS.fromString(osname, arch);

        return os;
    }
}