// Auth session support
#import <AuthenticationServices/AuthenticationServices.h>

#ifdef RCT_NEW_ARCH_ENABLED

#import <RNSwanBrowserSpec/RNSwanBrowserSpec.h>

// ASWebAuthenticationPresentationContextProviding provides presentation anchor for auth sessions
@interface RNSwanBrowser : NSObject <NativeRNSwanBrowserSpec, ASWebAuthenticationPresentationContextProviding>
@end

#else

#import <React/RCTBridgeModule.h>

// ASWebAuthenticationPresentationContextProviding provides presentation anchor for auth sessions
@interface RNSwanBrowser : NSObject <RCTBridgeModule, ASWebAuthenticationPresentationContextProviding>
@end

#endif
