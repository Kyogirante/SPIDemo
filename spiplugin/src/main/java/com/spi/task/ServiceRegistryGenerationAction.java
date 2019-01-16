package com.spi.task;

import com.spi.annotations.ServiceProvider;
import com.spi.loader.ServiceLoader;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.lang.model.element.Modifier;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.MemberValue;

/**
 * The action for {@code ServiceRegistry} generating
 *
 * @author KyoWang
 * @since 2017/08/25
 */

class ServiceRegistryGenerationAction {
    private final FileCollection classpath;

    private final File serviceDir;

    private final File sourcesDir;

    private final ClassPool pool;

    public ServiceRegistryGenerationAction(final FileCollection classpath, final File servicesDir, final File sourceDir) {
        this.classpath = classpath;
        this.serviceDir = servicesDir;
        this.sourcesDir = sourceDir;
        this.pool = new ClassPool(true) {
            @Override
            public ClassLoader getClassLoader() {
                return new Loader(this);
            }
        };
    }

    private List<CtClass> loadClasses() throws NotFoundException, IOException {
        final List<CtClass> classes = new LinkedList<CtClass>();

        for (final File file : this.classpath) {
            this.pool.appendClassPath(file.getAbsolutePath());
        }

        for (final File file : this.classpath) {
            loadClasses(this.pool, classes, file);
        }

        return classes;
    }

    private List<CtClass> loadClasses(final ClassPool pool, final List<CtClass> classes, final File file) throws IOException {
        final Stack<File> stack = new Stack<File>();
        stack.push(file);

        while (!stack.isEmpty()) {
            final File f = stack.pop();

            if (f.isDirectory()) {
                final File[] files = f.listFiles();
                if (null != files) {
                    for (final File child : files) {
                        stack.push(child);
                    }
                }
            } else if (f.getName().endsWith(".class")) {
                FileInputStream stream = null;

                try {
                    stream = new FileInputStream(f);
                    classes.add(pool.makeClass(stream));
                } finally {
                    if (null != stream) {
                        try {
                            stream.close();
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (f.getName().endsWith(".jar")) {
                loadClasses(pool, classes, new JarFile(f));
            }
        }

        return classes;
    }

    private List<CtClass> loadClasses(final ClassPool pool, final List<CtClass> classes, final JarFile jar) throws IOException {
        InputStream stream = null;

        final Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                try {
                    stream = jar.getInputStream(entry);
                    classes.add(pool.makeClass(stream));
                } finally {
                    if (null != stream) {
                        stream.close();
                    }
                }
            }
        }

        return classes;
    }

    public boolean execute() {
        try {

            deleteTempFile(this.serviceDir);

            final List<CtClass> classes = loadClasses();
            if (null == classes || classes.isEmpty()) {
                System.out.println("No class found");
                return false;
            }

            for (final CtClass cc : classes) {
                processClass(cc);
            }

            generateSourceCode();
        } catch (Exception e) {
            throw new GradleException("Could not generate ServiceRegistry", e);
        }

        return true;
    }

    private void generateSourceCode() throws IOException {
        final TypeName java_lang_Class = ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
        final ClassName java_util_Collections = ClassName.get("java.util", "Collections");
        final ClassName java_util_Map = ClassName.get("java.util", "Map");
        final ClassName java_util_Set = ClassName.get("java.util", "Set");
        final ClassName java_util_LinkedHashMap = ClassName.get("java.util", "LinkedHashMap");
        final ClassName java_util_HashSet = ClassName.get("java.util", "LinkedHashSet");
        final TypeName setOfClass = ParameterizedTypeName.get(java_util_Set, java_lang_Class);
        final TypeName hashSetOfClass = ParameterizedTypeName.get(java_util_HashSet, java_lang_Class);
        final TypeName mapOfClassToSetOfClass = ParameterizedTypeName.get(java_util_Map, java_lang_Class, setOfClass);
        final TypeName hashMapOfClassToSetOfClass = ParameterizedTypeName.get(java_util_LinkedHashMap, java_lang_Class, setOfClass);
        final TypeSpec tsServiceRegistry = TypeSpec.classBuilder("ServiceRegistry")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addField(FieldSpec.builder(mapOfClassToSetOfClass, "sServices")
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", hashMapOfClassToSetOfClass)
                        .build())
                .addStaticBlock(generateStaticInitializer())
                .addMethod(MethodSpec.methodBuilder("register")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.SYNCHRONIZED)
                        .addParameter(Class.class, "serviceClass", Modifier.FINAL)
                        .addParameter(Class.class, "providerClass", Modifier.FINAL)
                        .returns(TypeName.VOID)
                        .addCode(CodeBlock.builder()
                                .addStatement("$T providers = sServices.get(serviceClass)", setOfClass)
                                .beginControlFlow("if (null == providers)")
                                .addStatement("providers = new $T()", hashSetOfClass)
                                .endControlFlow()
                                .addStatement("providers.add(providerClass)")
                                .addStatement("sServices.put(serviceClass, providers)")
                                .build())
                        .build())
                .addMethod(MethodSpec.methodBuilder("get")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.SYNCHRONIZED)
                        .addParameter(Class.class, "clazz", Modifier.FINAL)
                        .returns(setOfClass)
                        .addCode(CodeBlock.builder()
                                .addStatement("final $T providers = sServices.get(clazz)", setOfClass)
                                .addStatement("return null == providers ? $T.<Class<?>>emptySet() : $T.unmodifiableSet(providers)", java_util_Collections, java_util_Collections)
                                .build())
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .build();
        JavaFile.builder(ServiceLoader.class.getPackage().getName(), tsServiceRegistry)
                .build()
                .writeTo(this.sourcesDir);
    }

    private CodeBlock generateStaticInitializer() throws IOException {
        final CodeBlock.Builder cinitBuilder = CodeBlock.builder();
        final File[] spis = this.serviceDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isFile();
            }
        });

        if (null != spis && spis.length > 0) {
            for (final File spi : spis) {
                final List<SpiElement> elements = new ArrayList<SpiElement>();
                final BufferedReader reader = new BufferedReader(new FileReader(spi));

                try {
                    for (String line; null != (line = reader.readLine()); ) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#"))
                            continue;

                        final StringTokenizer tokenizer = new StringTokenizer(line);
                        final String name = tokenizer.nextToken();
                        if (tokenizer.hasMoreTokens()) {
                            elements.add(new SpiElement(name, tokenizer.nextToken()));
                        } else {
                            elements.add(new SpiElement(name, 0));
                        }
                    }
                } finally {
                    reader.close();
                }

                Collections.sort(elements); // sort by priority asc

                for (final SpiElement se : elements) {
                    cinitBuilder.addStatement("register($L, $L)", spi.getName() + ".class", se.name + ".class");
                }
            }
        }

        return cinitBuilder.build();
    }

    private void processClass(final CtClass cc) throws IOException {
        if (!cc.hasAnnotation(ServiceProvider.class)) {
            return;
        }

        final ClassFile cf = cc.getClassFile();
        final Annotation annotation = getServiceProviderAnnotation(cf);
        if (null == annotation) {
            return;
        }

        final ArrayMemberValue value = (ArrayMemberValue) annotation.getMemberValue("value");
        final IntegerMemberValue priority = (IntegerMemberValue) annotation.getMemberValue("priority");
        final int priorityValue = null != priority ? priority.getValue() : 0;

        for (final MemberValue mv : value.getValue()) {
            final ClassMemberValue cmv = (ClassMemberValue) mv;
            final String serviceName = cmv.getValue();
            final File spi = new File(this.serviceDir, serviceName);

            if (!spi.exists()) {
                spi.createNewFile();
            }

            final PrintWriter out = new PrintWriter(new FileWriter(spi, true));
            out.printf("%s %d", cc.getName(), priorityValue).println();
            out.flush();
            out.close();
        }
    }

    private Annotation getServiceProviderAnnotation(final ClassFile cf) {
        final AnnotationsAttribute visibleAttr = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.visibleTag);
        if (null != visibleAttr) {
            final Annotation sp = visibleAttr.getAnnotation(ServiceProvider.class.getName());
            if (null != sp) {
                return sp;
            }
        }

        final AnnotationsAttribute invisibleAttr = (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.invisibleTag);
        if (null != invisibleAttr) {
            final Annotation sp = invisibleAttr.getAnnotation(ServiceProvider.class.getName());
            if (null != sp) {
                return sp;
            }
        }

        return null;
    }

    private static final class SpiElement implements Comparable<SpiElement> {
        final String name;
        final int priority;

        private SpiElement(final String name, final int priority) {
            this.name = name;
            this.priority = priority;
        }

        private SpiElement(final String name, final String priority) {
            this(name, parsePriority(priority));
        }

        @Override
        public int compareTo(final SpiElement o) {
            return this.priority > o.priority ? 1 : this.priority < o.priority ? -1 : 0;
        }

        private static final int parsePriority(final String s) {
            try {
                return Integer.parseInt(s);
            } catch (final NumberFormatException e) {
                return 0;
            }
        }
    }

    private void deleteTempFile(File dir) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                deleteTempFile(new File(dir, children[i]));
            }
        } else {
            dir.delete();
        }
    }
}
