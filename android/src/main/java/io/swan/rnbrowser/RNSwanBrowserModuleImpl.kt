package io.swan.rnbrowser

import android.content.Intent
import android.os.Handler
import android.os.Looper

import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

import io.swan.rnbrowser.helpers.CustomTabActivityHelper

// Implements LifecycleEventListener to detect when user cancels auth by closing Custom Tabs
object RNSwanBrowserModuleImpl : LifecycleEventListener {
  const val NAME = "RNSwanBrowser"

  // Auth session state
  private var authPromise: Promise? = null
  private var isAuthSessionActive: Boolean = false
  private var isInitialized: Boolean = false

  // Initialize lifecycle listener - called from module constructors
  fun initialize(context: ReactApplicationContext) {
    if (isInitialized) return
    isInitialized = true
    context.addLifecycleEventListener(this)
  }

  // When app resumes after auth session, check if user cancelled
  override fun onHostResume() {
    if (!isAuthSessionActive) return
    isAuthSessionActive = false

    // Delay to let JS Linking handler process first if this is a redirect
    Handler(Looper.getMainLooper()).postDelayed({
      authPromise?.let {
        // Promise still pending = user closed Custom Tabs without completing auth
        cancelAuthSession()
      }
    }, 200)
  }

  override fun onHostPause() {}

  override fun onHostDestroy() {
    cleanup()
    isInitialized = false
  }

  internal fun open(
    reactContext: ReactApplicationContext,
    url: String,
    options: ReadableMap,
    promise: Promise
  ) {
    val activity = reactContext.currentActivity
      ?: return promise.reject(
        "no_current_activity",
        "Couldn't call open() when the app is in background"
      )

    val intentBuilder = CustomTabsIntent.Builder().apply {
      setBookmarksButtonEnabled(false)
      setDownloadButtonEnabled(false)
      setInstantAppsEnabled(false)
      setSendToExternalDefaultHandlerEnabled(false)
      setShowTitle(false)

      if (options.getString("animationType") == "fade") {
        setStartAnimations(activity, com.facebook.react.R.anim.catalyst_fade_in, R.anim.inert)
        setExitAnimations(activity, R.anim.inert, com.facebook.react.R.anim.catalyst_fade_out)
      } else {
        setStartAnimations(activity, com.facebook.react.R.anim.catalyst_slide_up, R.anim.inert)
        setExitAnimations(activity, R.anim.inert, com.facebook.react.R.anim.catalyst_slide_down)
      }
    }

    @ColorInt val blackColor = ContextCompat.getColor(activity ,android.R.color.black)

    val paramsBuilder = CustomTabColorSchemeParams.Builder().apply {
      setNavigationBarColor(blackColor)

      if (options.hasKey("barTintColor")) {
        @ColorInt val barTintColor = options.getInt("barTintColor")

        setToolbarColor(barTintColor)
        setSecondaryToolbarColor(barTintColor)

        intentBuilder.setColorScheme(
          when (ColorUtils.calculateLuminance(barTintColor) > 0.5) {
            true -> CustomTabsIntent.COLOR_SCHEME_LIGHT
            false -> CustomTabsIntent.COLOR_SCHEME_DARK
          }
        )
      }
    }

    intentBuilder.setDefaultColorSchemeParams(paramsBuilder.build())

    val customTabsIntent = intentBuilder.build().apply {
      intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
      intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    CustomTabActivityHelper.openCustomTab(
      activity, customTabsIntent, url.toUri()
    ) { currentActivity, uri ->
      currentActivity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    promise.resolve(null)
  }

  // Open auth session using Custom Tabs, resolves with cancel if user closes
  internal fun openAuthSession(
    reactContext: ReactApplicationContext,
    url: String,
    redirectUrl: String,
    options: ReadableMap,
    promise: Promise
  ) {
    if (authPromise != null) {
      return promise.reject(
        "auth_session_in_progress",
        "An auth session is already in progress"
      )
    }

    val activity = reactContext.currentActivity
      ?: return promise.reject(
        "no_current_activity",
        "Couldn't call openAuthSession() when the app is in background"
      )

    authPromise = promise
    isAuthSessionActive = true

    // Check if ephemeral session is preferred (defaults to true for auth)
    val prefersEphemeral = if (options.hasKey("prefersEphemeralSession")) {
      options.getBoolean("prefersEphemeralSession")
    } else {
      true
    }

    val intentBuilder = CustomTabsIntent.Builder().apply {
      setBookmarksButtonEnabled(false)
      setDownloadButtonEnabled(false)
      setInstantAppsEnabled(false)
      setSendToExternalDefaultHandlerEnabled(false)
      setShowTitle(false)
      setStartAnimations(activity, com.facebook.react.R.anim.catalyst_slide_up, R.anim.inert)
      setExitAnimations(activity, R.anim.inert, com.facebook.react.R.anim.catalyst_slide_down)

      // setEphemeralBrowsingEnabled requires androidx.browser 1.9.0+, call dynamically for backwards compatibility
      if (prefersEphemeral) {
        try {
          javaClass.getMethod("setEphemeralBrowsingEnabled", Boolean::class.javaPrimitiveType)
            .invoke(this, true)
        } catch (e: NoSuchMethodException) {
          // androidx.browser < 1.9.0, ephemeral not available
        }
      }
    }

    val customTabsIntent = intentBuilder.build().apply {
      intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
      intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    CustomTabActivityHelper.openCustomTab(
      activity, customTabsIntent, url.toUri()
    ) { currentActivity, uri ->
      currentActivity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
  }

  // Cancel in-progress auth session
  internal fun cancelAuthSession() {
    val promise = authPromise ?: return
    val result = Arguments.createMap().apply {
      putString("type", "cancel")
    }
    promise.resolve(result)
    cleanup()
  }

  // Clean up auth session state
  private fun cleanup() {
    authPromise = null
  }
}
