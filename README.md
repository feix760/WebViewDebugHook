# WebViewDebugHook

From https://developers.google.com/web/tools/chrome-devtools/remote-debugging we can use blow code to debugging webview on android 4.4+.

```java
    WebView.setWebContentsDebuggingEnabled(true);
```
But the most app are third parties, webview debugging is disabled, `WebViewDebugHook` can force them to debugging.

## Requirement

- Android 4.4+

- Rooted device

- Installed [Xposed](http://repo.xposed.info/module/de.robv.android.xposed.installer)

## Xposed module Download

- [WebViewDebugHook.apk](app/release/app-release.apk?raw=true)

## Supported Webview

- Android Webview
- Tencent X5 Webview
- UC U4/U3 Webview
- XWalkView (微信小程序)

*You can post an issue to support more webview*

## Question

- What is Xposed ?

Xposed is a framework for modules that can change the behavior of the system and apps without touching any APKs. That's great because it means that modules can work for different versions and even ROMs without any changes (as long as the original code was not changed too much). It's also easy to undo. As all changes are done in the memory, you just need to deactivate the module and reboot to get your original system back. There are many other advantages, but here is just one more: Multiple modules can do changes to the same part of the system or app. With modified APKs, you to decide for one. No way to combine them, unless the author builds multiple APKs with different combinations.
<br><br>Please go http://repo.xposed.info/module/de.robv.android.xposed.installer for details.

## Links

- [Xposed Development tutorial](https://github.com/rovo89/XposedBridge/wiki/Development-tutorial)
- [Tencent X5 SDK](https://x5.tencent.com/tbs/guide/sdkInit.html)

## License

(The MIT License)

