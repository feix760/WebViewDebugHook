package club.fishine.webviewdebughook;


import java.security.SecureClassLoader;
import java.util.HashMap;

import dalvik.system.BaseDexClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class WebViewHook implements IXposedHookLoadPackage {

    private HashMap<String, LRUCache<String, Boolean>> hookedClassLoader = new HashMap<>();

    private boolean markClassLoaderHooked(final String packageName, ClassLoader classLoader) {
        LRUCache<String, Boolean> maps = hookedClassLoader.get(packageName);
        if (maps == null) {
            maps = new LRUCache<>(10000);
            hookedClassLoader.put(packageName, maps);
        }
        String loaderHash = "" + classLoader.hashCode();

        if (maps.get(loaderHash) == null) {
            maps.put(loaderHash, true);
            return true;
        }
        return false;
    }

    private void hookWebView(final ClassLoader classLoader, final String packageName) {
        final String[] webviewList = {
                "android.webkit.WebView", // android webview
                "com.tencent.smtt.sdk.WebView",  // tencent x5
                "com.uc.webview.export.WebView", // UC
        };
        for (int i = 0; i < webviewList.length; i++) {
            final String className = webviewList[i];

            Class findCla = null;

            try {
                findCla = XposedHelpers.findClass(className, classLoader); // will throw exception
            } catch (Throwable exception) {
            }

            if (findCla != null && markClassLoaderHooked(packageName, findCla.getClassLoader())) {
                final Class cla = findCla;

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

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        final String packageName = lpparam.packageName;

        if (packageName.equals("com.android.webview")) {
            return;
        }

        XposedBridge.log(packageName + " load");

        hookWebView(lpparam.classLoader, packageName);

        Class[] loaderClassList = {
                BaseDexClassLoader.class,
                SecureClassLoader.class,
        };

        for (int i = 0; i < loaderClassList.length; i++) {
            final Class loaderClass = loaderClassList[i];

            XposedBridge.hookAllConstructors(loaderClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ClassLoader classLoader = (ClassLoader) param.thisObject;

                    XposedBridge.log(packageName + " new " + classLoader.getClass().getName() + "()");

                    hookWebView(classLoader, packageName);
                }
            });
        }
    }
}
