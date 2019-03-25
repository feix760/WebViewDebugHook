package club.fishine.webviewdebughook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class WebViewHook implements IXposedHookLoadPackage {
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        final String packageName = lpparam.packageName;

        if (packageName.equals("com.android.webview")) {
            return;
        }

        // android webview & tencent x5 & UC
        final String[] webviewList = {"android.webkit.WebView", "com.tencent.smtt.sdk.WebView", "com.uc.webview.export.WebView"};
        for (int i = 0; i < webviewList.length; i++) {

            final String className = webviewList[i];

            Class findCla = null;

            try {
                findCla = XposedHelpers.findClass(className, lpparam.classLoader); // will throw exception
            } catch (Throwable exception) {
            }

            if (findCla != null) {
                final Class cla = findCla;

                XposedBridge.hookAllConstructors(cla, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedHelpers.callStaticMethod(cla, "setWebContentsDebuggingEnabled", true);
                        XposedBridge.log(className + "(), " + packageName);
                    }
                });

                XposedBridge.hookAllMethods(cla, "setWebContentsDebuggingEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] != Boolean.TRUE) {
                            param.args[0] = true;
                            XposedBridge.log(className + ".setWebContentsDebuggingEnabled(), " + packageName);
                        }
                    }
                });
            }
        }

    }
}
