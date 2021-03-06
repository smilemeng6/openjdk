/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.compiler.test;

import java.lang.reflect.Method;

/**
 * Facade for the {@code java.lang.reflect.Module} class introduced in JDK9 that allows tests to be
 * developed against JDK8 but use module logic if deployed on JDK9.
 */
public class JLRModule {

    static {
        if (GraalTest.Java8OrEarlier) {
            throw new AssertionError("Use of " + JLRModule.class + " only allowed if " + GraalTest.class.getName() + ".JDK8OrEarlier is false");
        }
    }

    private final Object realModule;

    public JLRModule(Object module) {
        this.realModule = module;
    }

    private static final Class<?> moduleClass;
    private static final Method getModuleMethod;
    private static final Method getUnnamedModuleMethod;
    private static final Method getPackagesMethod;
    private static final Method isExportedMethod;
    private static final Method isExported2Method;
    private static final Method addExportsMethod;
    static {
        try {
            moduleClass = Class.forName("java.lang.reflect.Module");
            getModuleMethod = Class.class.getMethod("getModule");
            getUnnamedModuleMethod = ClassLoader.class.getMethod("getUnnamedModule");
            getPackagesMethod = moduleClass.getMethod("getPackages");
            isExportedMethod = moduleClass.getMethod("isExported", String.class);
            isExported2Method = moduleClass.getMethod("isExported", String.class, moduleClass);
            addExportsMethod = moduleClass.getMethod("addExports", String.class, moduleClass);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static JLRModule fromClass(Class<?> cls) {
        try {
            return new JLRModule(getModuleMethod.invoke(cls));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static JLRModule getUnnamedModuleFor(ClassLoader cl) {
        try {
            return new JLRModule(getUnnamedModuleMethod.invoke(cl));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Exports all packages in this module to a given module.
     */
    public void exportAllPackagesTo(JLRModule module) {
        if (this != module) {
            for (String pkg : getPackages()) {
                // Export all JVMCI packages dynamically instead
                // of requiring a long list of -XaddExports
                // options on the JVM command line.
                if (!isExported(pkg, module)) {
                    addExports(pkg, module);
                }
            }
        }
    }

    public String[] getPackages() {
        try {
            return (String[]) getPackagesMethod.invoke(realModule);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public boolean isExported(String pn) {
        try {
            return (Boolean) isExportedMethod.invoke(realModule, pn);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public boolean isExported(String pn, JLRModule other) {
        try {
            return (Boolean) isExported2Method.invoke(realModule, pn, other.realModule);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public void addExports(String pn, JLRModule other) {
        try {
            addExportsMethod.invoke(realModule, pn, other.realModule);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
