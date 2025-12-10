package io.swan.rnbrowser

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = RNSwanBrowserModuleImpl.NAME)
class RNSwanBrowserModule(reactContext: ReactApplicationContext) :
  NativeRNSwanBrowserSpec(reactContext) {

  init {
    // Register lifecycle listener to detect when user cancels auth session
    RNSwanBrowserModuleImpl.initialize(reactContext)
  }

  override fun getName(): String {
    return RNSwanBrowserModuleImpl.NAME
  }

  override fun open(url: String, options: ReadableMap, promise: Promise) {
    RNSwanBrowserModuleImpl.open(reactApplicationContext, url, options, promise)
  }

  override fun close() {
    // noop on Android since the modal is closed by deep-link
  }

  // Auth session methods
  override fun openAuthSession(url: String, redirectUrl: String, options: ReadableMap, promise: Promise) {
    RNSwanBrowserModuleImpl.openAuthSession(reactApplicationContext, url, redirectUrl, options, promise)
  }

  override fun cancelAuthSession() {
    RNSwanBrowserModuleImpl.cancelAuthSession()
  }
}
