import { Platform, processColor } from "react-native";
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
// Uses ASWebAuthenticationSession on iOS which handles redirects natively
export const openAuthSession = (
  url: string,
  redirectUrl: string,
  options: AuthSessionOptions = {},
): Promise<AuthSessionResult> => {
  if (Platform.OS !== "ios") {
    return Promise.reject(new Error("openAuthSession is only supported on iOS"));
  }

  return NativeModule.openAuthSession(url, redirectUrl, {
    prefersEphemeralSession: options.prefersEphemeralSession ?? true,
  }) as Promise<AuthSessionResult>;
};

// Cancel any in-progress auth session
export const cancelAuthSession = (): void => {
  NativeModule.cancelAuthSession();
};
