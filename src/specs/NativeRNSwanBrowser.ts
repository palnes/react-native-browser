import type { TurboModule } from "react-native";
import { TurboModuleRegistry } from "react-native";

type Options = {
  animationType?: string;
  dismissButtonStyle?: string;
  barTintColor?: number;
  controlTintColor?: number;
};

// Auth session options
type AuthSessionOptions = {
  prefersEphemeralSession?: boolean;
};

// Auth session result - consistent across platforms
// type: "success" | "cancel", url only present on success
type AuthSessionResult = {
  type: string;
  url?: string;
};

export interface Spec extends TurboModule {
  open(url: string, options: Options): Promise<null>;
  close(): void;

  // Auth session methods
  openAuthSession(
    url: string,
    redirectUrl: string,
    options: AuthSessionOptions,
  ): Promise<AuthSessionResult>;
  cancelAuthSession(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>("RNSwanBrowser");
