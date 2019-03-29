package club.fishine.webviewdebughook;


import dalvik.system.BaseDexClassLoader;

import java.security.SecureClassLoader;
import java.util.HashMap;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class WebViewHook implements IXposedHookLoadPackage {

    private HashMap<String, LRUCache<String, Boolean>> hookedClassLoader = new HashMap<>();

    private boolean markClassLoaderHooked(final String packageName, String name, ClassLoader classLoader) {
        LRUCache<String, Boolean> maps = hookedClassLoader.get(packageName);
        if (maps == null) {
            maps = new LRUCache<>(10000);
            hookedClassLoader.put(packageName, maps);
        }
        String key = name + "@" + classLoader.hashCode();

        if (maps.get(key) == null) {
            maps.put(key, true);
            return true;
        }
        return false;
    }

    private Class findClass(String className, ClassLoader classLoader) {
        try {
            return XposedHelpers.findClass(className, classLoader);
        } catch (Throwable exception) {
        }
        return null;
    }

    private Method findMethod(Class cla, String methodName) {
        try {
            return XposedHelpers.findMethodExact(cla, methodName);
        } catch (Throwable exception) {
        }
        return null;
    }

    private void hookAll(final ClassLoader classLoader, final String packageName) {
        hookWebView(classLoader, packageName);

        hookXWalkView(classLoader, packageName);

        if (packageName.equals("com.taobao.taobao")) {
            hookTaobao(classLoader, packageName);
        }
    }

    private void hookWebView(final ClassLoader classLoader, final String packageName) {
        final String[] webviewList = {
                "android.webkit.WebView", // android webview
                "com.tencent.smtt.sdk.WebView",  // tencent x5
                "com.uc.webview.export.WebView", // UC
        };
        for (final String className : webviewList) {

            final Class cla = this.findClass(className, classLoader);

            if (cla != null && markClassLoaderHooked(packageName, className, cla.getClassLoader())) {

                XposedBridge.log(packageName + " hook " + className + "@" + cla.getClassLoader().getClass().getName() + ":" + cla.getClassLoader().hashCode());

                XposedBridge.hookAllConstructors(cla, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log(packageName + " new " + className + "()");

                        XposedHelpers.callStaticMethod(cla, "setWebContentsDebuggingEnabled", true);
                    }
                });

                XposedBridge.hookAllMethods(cla, "setWebContentsDebuggingEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log(packageName + " " + className + ".setWebContentsDebuggingEnabled(" + param.args[0].toString() + ")");
                        param.args[0] = true;
                    }
                });
            }
        }

    }

    private void hookXWalkView(final ClassLoader classLoader, final String packageName) {
        final String className = "org.xwalk.core.XWalkView";
        final String classNameXWalkPreferences = "org.xwalk.core.XWalkPreferences";
        final String REMOTE_DEBUGGING = "remote-debugging";

        final Class cla = this.findClass(className, classLoader);
        final Class claXWalkPreferences = this.findClass(classNameXWalkPreferences, classLoader);

        if (cla != null && markClassLoaderHooked(packageName, className, cla.getClassLoader())) {
            XposedBridge.log(packageName + " hook " + className + "@" + cla.getClassLoader().getClass().getName() + ":" + cla.getClassLoader().hashCode());

            XposedBridge.hookAllConstructors(cla, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(packageName + " new " + className + "()");

                    XposedHelpers.callStaticMethod(claXWalkPreferences, "setValue", REMOTE_DEBUGGING, true);
                }
            });

            XposedBridge.hookAllMethods(claXWalkPreferences, "setValue", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(packageName + " " + classNameXWalkPreferences + ".setValue(" + param.args[0].toString() + ", " + param.args[1].toString() + ")");
                    if (param.args[0].toString().equals(REMOTE_DEBUGGING)) {
                        param.args[1] = true;
                    }
                }
            });
        }
    }

    private void hookTaobao(final ClassLoader classLoader, final String packageName) {
        final String className = "c8.STbr";
        final String methodName = "isAppDebug";
        final Class cla = this.findClass(className, classLoader);
        if (cla != null && this.findMethod(cla, methodName) != null && markClassLoaderHooked(packageName, className, cla.getClassLoader())) {

            XposedBridge.log(packageName + " hook " + className + "." + methodName + "()@" + cla.getClassLoader().getClass().getName() + ":" + cla.getClassLoader().hashCode());

            XposedBridge.hookAllMethods(cla, methodName, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(packageName + " " + className + "." + methodName + "()");
                    param.setResult(true);
                }
            });
        }
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        final String packageName = lpparam.packageName;

        if (packageName.equals("com.android.webview")) {
            return;
        }

        XposedBridge.log(packageName + " load");

        hookAll(lpparam.classLoader, packageName);

        Class[] loaderClassList = {
                BaseDexClassLoader.class,
                SecureClassLoader.class,
        };

        for (final Class loaderClass : loaderClassList) {
            XposedBridge.hookAllConstructors(loaderClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ClassLoader classLoader = (ClassLoader) param.thisObject;

                    XposedBridge.log(packageName + " new " + classLoader.getClass().getName() + "()");

                    hookAll(classLoader, packageName);
                }
            });
        }

    }
}
