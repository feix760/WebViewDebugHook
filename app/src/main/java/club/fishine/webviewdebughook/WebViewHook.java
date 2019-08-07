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

    private LRUCache<String, Boolean> hookedClassLoader = new LRUCache<>(10000);

    private boolean markClassLoaderHooked(Class cla) {
        String key = cla.getName() + "@" + cla.getClassLoader().hashCode();
        if (hookedClassLoader.get(key) == null) {
            hookedClassLoader.put(key, true);
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
            for (Method method : cla.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
        } catch (Throwable exception) {
        }
        return null;
    }

    private void hookClassLoaderEntry(final LoadPackageParam lpparam) {
        final String packageName = lpparam.packageName;
        Class[] loaderClassList = {
                BaseDexClassLoader.class,
                SecureClassLoader.class,
        };

        for (final Class loaderClass : loaderClassList) {
            XposedBridge.hookAllConstructors(loaderClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ClassLoader classLoader = (ClassLoader) param.thisObject;

                    // XposedBridge.log(packageName + " new " + classLoader.getClass().getName() + ":" + classLoader.hashCode());

                    hookAll(classLoader, packageName);
                }
            });
        }
    }

    // tencent StubShell
    private void hookTxAppEntry(final LoadPackageParam lpparam) {
        final String packageName = lpparam.packageName;
        final Class TxAppEntry = findClass("com.tencent.StubShell.TxAppEntry", lpparam.classLoader);
        final String TxAppEntryMethod = "attachBaseContext";

        if (TxAppEntry != null && findMethod(TxAppEntry, TxAppEntryMethod) != null) {
            XposedBridge.hookAllMethods(TxAppEntry, TxAppEntryMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object context = param.args[0];

                    ClassLoader classLoader = (ClassLoader) context.getClass().getMethod("getClassLoader").invoke(context);

                    XposedBridge.log(packageName + " com.tencent.StubShell new " + classLoader.getClass().getName() + ":" + classLoader.hashCode());

                    hookAll(classLoader, packageName);
                }
            });
        }
    }

    private void hookAll(final ClassLoader classLoader, final String packageName) {
        hookWebView(classLoader, packageName);

        hookXWalkView(classLoader, packageName);
    }

    private void hookWebView(final ClassLoader classLoader, final String packageName) {
        final String[] webviewList = {
                "android.webkit.WebView", // android webview
                "com.tencent.smtt.sdk.WebView",  // tencent x5
                "com.uc.webview.export.WebView", // UC
        };
        for (final String className : webviewList) {

            final Class cla = this.findClass(className, classLoader);

            if (cla != null && markClassLoaderHooked(cla)) {
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

        final Class cla = findClass(className, classLoader);
        final Class claXWalkPreferences = findClass(classNameXWalkPreferences, classLoader);

        if (cla != null && markClassLoaderHooked(cla)) {
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


    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        final String packageName = lpparam.packageName;

        if (packageName.equals("com.android.webview")) {
            return;
        }

        XposedBridge.log(packageName + " load");

        hookAll(lpparam.classLoader, packageName);

        hookTxAppEntry(lpparam);

        hookClassLoaderEntry(lpparam);
    }
}