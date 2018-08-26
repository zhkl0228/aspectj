/*******************************************************************************
 * Copyright (c) 2005 Contributors.
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Vasseur         initial implementation
 *******************************************************************************/
package org.aspectj.weaver.loadtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Java 1.5 preMain agent to hook in the class pre processor
 * Can be used with -javaagent:aspectjweaver.jar
 *
 * @author Alexandre Vasseur
 * @author Alexander Kriegisch
 */
public class Agent { 

    /**
     * The instrumentation instance
     */
    private static Instrumentation s_instrumentation;

    /**
     * JSR-163 preMain Agent entry method
     *
     * @param options
     * @param instrumentation
     */
    public static void premain(String options, Instrumentation instrumentation) {
    	/* Handle duplicate agents */
    	if (s_instrumentation != null) {
    		return;
    	}
        s_instrumentation = instrumentation;
        File jarFile = getJarFile();
        initClassPath(instrumentation, jarFile);

        processClassPath(instrumentation);

        s_instrumentation.addTransformer(new ClassPreProcessorAgentAdapter(Agent.class.getClassLoader()));
    }

    private static void processClassPath(Instrumentation instrumentation) {
        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(File.pathSeparator);
        final List<File> list = new ArrayList<File>(10);
        boolean flag = false;
        try {
            for ( String entry : entries ) {
                File file = new File(entry);
                if (file.isDirectory()) {
                    list.add(file);
                    flag = true;
                } else if (flag) {
                    // System.out.println("Append to bootstrap class loader: " + file);
                    instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(file));
                }
            }

            if (!list.isEmpty()) {
                File tmpJar = File.createTempFile("aspectj_cp_", ".jar");
                zip(tmpJar, list.toArray(new File[0]));
                // System.out.println("Append to bootstrap class loader: " + tmpJar);
                instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(tmpJar));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void agentmain(String options, Instrumentation instrumentation) {
        premain(options, instrumentation);
    }

    /**
     * Returns the Instrumentation system level instance
     */
    public static Instrumentation getInstrumentation() {
        if (s_instrumentation == null) {
            throw new UnsupportedOperationException(
                "AspectJ weaving agent was neither started via '-javaagent' (preMain) " +
                "nor attached via 'VirtualMachine.loadAgent' (agentMain)");
        }
        return s_instrumentation;
    }

    /**
     * The agent is loaded in the App class loader. Instrumented
     * classes are in the boot class loader so can't see "MasterSecretCallback"
     * by default. Adding self to the boot class loader will make
     * MasterSecretCallback visible to core classes. Not that this leads
     * to a split-brain state where some classes of the jar are loaded
     * by the App class loader and some in the boot class loader.
     */
    private static void initClassPath(Instrumentation inst, File jarFile) {
        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(jarFile));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static File getJarFile() {
        URL jarUrl = Agent.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return new File(jarUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void zip(File zipFile, File...directories) throws IOException  {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
        for (File directory : directories) {
            addDir(directory.getAbsolutePath(), directory, out);
        }
        out.close();
    }

    private static void addDir(String root, File directory, ZipOutputStream out) throws IOException {
        File[] files = directory.listFiles();
        final byte[] tmpBuf = new byte[1024];

        for (int i = 0; files != null && i < files.length; i++) {
            if (files[i].isDirectory()) {
                addDir(root, files[i], out);
                continue;
            }

            if (files[i].getName().endsWith(".jar")) {
                continue;
            }

            FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
            String fileWithoutRootDir = files[i].getAbsolutePath();
            fileWithoutRootDir = fileWithoutRootDir.substring(root.length()+1);
            fileWithoutRootDir = fileWithoutRootDir.replaceAll("\\\\", "/");

            ZipEntry entry = new ZipEntry(fileWithoutRootDir);

            out.putNextEntry(entry);
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();

        }
    }

}
