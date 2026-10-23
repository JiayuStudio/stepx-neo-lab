// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Divinakra
import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/** Build-time tool: copies asm-9.7.jar into a new jar with its package renamed. */
public class RelocateAsm {
    public static void main(String[] a) throws Exception {
        final String from = "org/objectweb/asm/";
        final String to = a[2];
        Remapper r = new Remapper() {
            @Override public String map(String n) { return n.startsWith(from) ? to + n.substring(from.length()) : n; }
        };
        try (JarFile in = new JarFile(a[0]);
             JarOutputStream out = new JarOutputStream(new FileOutputStream(a[1]))) {
            for (Enumeration<JarEntry> e = in.entries(); e.hasMoreElements();) {
                JarEntry je = e.nextElement();
                String n = je.getName();
                if (!n.startsWith(from) || !n.endsWith(".class")) continue;
                ClassReader cr = new ClassReader(in.getInputStream(je));
                ClassWriter cw = new ClassWriter(0);
                cr.accept(new ClassRemapper(cw, r), 0);
                out.putNextEntry(new JarEntry(to + n.substring(from.length())));
                out.write(cw.toByteArray());
                out.closeEntry();
            }
        }
    }
}
