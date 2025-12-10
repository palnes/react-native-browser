import { Linking, Platform, processColor } from "react-native";
import NativeModule from "./specs/NativeRNSwanBrowser";

export type AnimationType = "fade" | "slide";
export type DismissButtonStyle = "cancel" | "close" | "done";

export type Options = {
  animationType?: AnimationType;
  dismissButtonStyle?: DismissButtonStyle;
  barTintColor?: string;
  controlTintColor?: string;
};

// Auth session types
export type AuthSessionOptions = {
  prefersEphemeralSession?: boolean;
};

export type AuthSessionResult = {
  type: "success" | "cancel";
  url?: string;
};

const convertColorToNumber = (
  color: string | undefined,
): number | undefined => {
  const processed = processColor(color);

  if (typeof processed === "number") {
    return processed;
  }
};

export const openBrowser = (
  url: string,
  options: Options = {},
): Promise<void> => {
  const { animationType, dismissButtonStyle } = options;
  const barTintColor = convertColorToNumber(options.barTintColor);
  const controlTintColor = convertColorToNumber(options.controlTintColor);

  return NativeModule.open(url, {
    ...(animationType != null && { animationType }),
    ...(dismissButtonStyle != null && { dismissButtonStyle }),
    ...(barTintColor != null && { barTintColor }),
    ...(controlTintColor != null && { controlTintColor }),
  }).then(() => {});
};

export const closeBrowser = (): void => {
  NativeModule.close();
};

// Open auth session for OAuth flows
// iOS: Uses ASWebAuthenticationSession which handles redirects natively
// Android: Uses Custom Tabs + Linking to intercept redirect URL
export const openAuthSession = (
  url: string,
  redirectUrl: string,
  options: AuthSessionOptions = {},
): Promise<AuthSessionResult> => {
  // iOS handles everything natively via ASWebAuthenticationSession
  if (Platform.OS === "ios") {
    return NativeModule.openAuthSession(url, redirectUrl, {
      prefersEphemeralSession: options.prefersEphemeralSession ?? true,
    }) as Promise<AuthSessionResult>;
  }

  // Android: Race between redirect URL and user cancel
  return new Promise((resolve) => {
    // Extract scheme from redirectUrl (e.g., "myapp://callback" -> "myapp")
    const schemeMatch = redirectUrl.match(/^([a-zA-Z][a-zA-Z0-9+.-]*):\/\//);
    const scheme = schemeMatch ? schemeMatch[1] : "";

    const handleUrl = ({ url: incomingUrl }: { url: string }) => {
      if (incomingUrl.startsWith(scheme + "://")) {
        cleanup();
        resolve({ type: "success", url: incomingUrl });
      }
    };

    const subscription = Linking.addEventListener("url", handleUrl);

    const cleanup = () => {
      subscription.remove();
    };

    // Open browser, native module will resolve with cancel if user closes
    NativeModule.openAuthSession(url, redirectUrl, {
      prefersEphemeralSession: options.prefersEphemeralSession ?? true,
    }).then((result) => {
      // Only handle cancel here, success is handled by Linking
      if ((result as AuthSessionResult).type === "cancel") {
        cleanup();
        resolve({ type: "cancel" });
      }
    });
  });
};

// Cancel any in-progress auth session
export const cancelAuthSession = (): void => {
  NativeModule.cancelAuthSession();
};
