package com.os.operando.garum;

import android.content.Context;

import com.os.operando.garum.models.PrefModel;
import com.os.operando.garum.serializers.DateSerializer;
import com.os.operando.garum.serializers.TypeSerializer;
import com.os.operando.garum.utils.GarumLog;
import com.os.operando.garum.utils.ReflectionUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexFile;

public class ModelInfo {

    private Map<Class<? extends PrefModel>, PrefInfo> prefInfos = new HashMap<Class<? extends PrefModel>, PrefInfo>();
    private Map<Class<?>, TypeSerializer> typeSerializers = new HashMap<Class<?>, TypeSerializer>() {
        {
            put(java.util.Date.class, new DateSerializer());
        }
    };


    public ModelInfo(Configuration configuration) throws IOException {
        if (!loadModelConfiguration(configuration)) {
            try {
                scanForModel(configuration.getContext());
            } catch (IOException e) {
                GarumLog.e("Couldn't open source path.", e);
            }
        }
        GarumLog.i("ModelInfo loaded.");
    }

    public TypeSerializer getTypeSerializer(Class<?> type) {
        return typeSerializers.get(type);
    }

    private void scanForModel(Context context, String packages) throws IOException {
        String packageName = context.getPackageName();
        String sourcePath = context.getApplicationInfo().sourceDir;
        List<String> paths = new ArrayList<String>();
        if (sourcePath != null && !(new File(sourcePath).isDirectory())) {
            DexFile dexfile = new DexFile(sourcePath);
            Enumeration<String> entries = dexfile.entries();
            while (entries.hasMoreElements()) {
                paths.add(entries.nextElement());
            }
        } else {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources("");
            while (resources.hasMoreElements()) {
                String path = resources.nextElement().getFile();
                if (path.contains("bin") || path.contains("classes")) {
                    paths.add(path);
                }
            }
        }
        for (String path : paths) {
            GarumLog.d("path : " + path);
            File file = new File(path);
            scanForModelClasses(file, packageName, context.getClassLoader());
        }
    }

    private void scanForModel(Context context) throws IOException {
        String packageName = context.getPackageName();
        String sourcePath = context.getApplicationInfo().sourceDir;
        List<String> paths = new ArrayList<String>();
        if (sourcePath != null && !(new File(sourcePath).isDirectory())) {
            DexFile dexfile = new DexFile(sourcePath);
            Enumeration<String> entries = dexfile.entries();
            while (entries.hasMoreElements()) {
                paths.add(entries.nextElement());
            }
        } else {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources("");
            while (resources.hasMoreElements()) {
                String path = resources.nextElement().getFile();
                if (path.contains("bin") || path.contains("classes")) {
                    paths.add(path);
                }
            }
        }
        for (String path : paths) {
            GarumLog.d("path : " + path);
            File file = new File(path);
            scanForModelClasses(file, packageName, context.getClassLoader());
        }
    }

    private void scanForModelClasses(File path, String packageName, ClassLoader classLoader) {
        if (path.isDirectory()) {
            for (File file : path.listFiles()) {
                scanForModelClasses(file, packageName, classLoader);
            }
        } else {
            String className = path.getName();
            if (!path.getPath().equals(className)) {
                className = path.getPath();
                if (className.endsWith(".class")) {
                    className = className.substring(0, className.length() - 6);
                } else {
                    return;
                }
                className = className.replace(System.getProperty("file.separator"), ".");
                int packageNameIndex = className.lastIndexOf(packageName);
                if (packageNameIndex < 0) {
                    return;
                }
                className = className.substring(packageNameIndex);
            }
            try {
                Class<?> discoveredClass = Class.forName(className, false, classLoader);
                if (ReflectionUtil.isModel(discoveredClass)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends PrefModel> modelClass = (Class<? extends PrefModel>) discoveredClass;
                    prefInfos.put(modelClass, new PrefInfo(modelClass));
                }
            } catch (ClassNotFoundException e) {
            }
        }
    }

    private boolean loadModelConfiguration(Configuration configuration) {
        if (!configuration.isValid()) {
            return false;
        }

        final List<Class<? extends PrefModel>> models = configuration.getModelClasses();
        if (models != null) {
            for (Class<? extends PrefModel> model : models) {
                prefInfos.put(model, new PrefInfo(model));
            }
        }

        final List<Class<? extends TypeSerializer>> typeSerializerList = configuration.getTypeSerializers();
        if (typeSerializers != null) {
            for (Class<? extends TypeSerializer> typeSerializer : typeSerializerList) {
                try {
                    TypeSerializer instance = typeSerializer.newInstance();
                    typeSerializers.put(instance.getDeserializedType(), instance);
                } catch (InstantiationException e) {
                    GarumLog.e("Couldn't instantiate TypeSerializer.", e);
                } catch (IllegalAccessException e) {
                    GarumLog.e("IllegalAccessException", e);
                }
            }
        }

        return true;
    }

    public Collection<PrefInfo> getPrefInfos() {
        return prefInfos.values();
    }

    public PrefInfo getPrefInfo(Class<? extends PrefModel> type) {
        return prefInfos.get(type);
    }
}
