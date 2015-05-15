# WebViewDebugHook

From [https://developer.chrome.com/devtools/docs/remote-debugging](https://developer.chrome.com/devtools/docs/remote-debugging) we can use blow code to debugging webview on android 4.4+.

```java
    WebView.setWebContentsDebuggingEnabled(true);
```
But the most app are third parties, webview debugging is disabled. WebViewDebugHook can force app to debugging.

## Requirement
- Android 4.4+

- Rooted device

- Installed xposed framework

## Note
- libs/XposedBridgeApi-XX.jar should not build to app file.

- Tencent QQ use it's X5 kernel, and could not debugging. We can make a file named `debug.conf` with blow content in the root directory to force it use webview.

```ini
    result_QProxy=false
    result_systemWebviewForceUsed=true
    setting_forceUseSystemWebview=true
```
