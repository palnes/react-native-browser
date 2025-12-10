#import "RNSwanBrowser.h"

#import <React/RCTConvert.h>
#import <SafariServices/SafariServices.h>

@interface RNSwanBrowser() <SFSafariViewControllerDelegate, UIAdaptivePresentationControllerDelegate>

@property (nonatomic, strong) SFSafariViewController *safariVC;
// Auth session instance
@property (nonatomic, strong) ASWebAuthenticationSession *authSession;

@end

@implementation RNSwanBrowser

RCT_EXPORT_MODULE();

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

- (void)handleOnClose {
  _safariVC = nil;
}

- (void)presentationControllerDidDismiss:(UIPresentationController *)controller {
  [self handleOnClose];
}

- (void)safariViewControllerDidFinish:(SFSafariViewController *)controller {
  [self handleOnClose];
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeRNSwanBrowserSpecJSI>(params);
}

- (void)open:(NSString *)url
     options:(JS::NativeRNSwanBrowser::Options &)options
     resolve:(RCTPromiseResolveBlock)resolve
      reject:(RCTPromiseRejectBlock)reject {

  NSString *animationType = options.animationType();
  NSString *dismissButtonStyle = options.dismissButtonStyle();
  NSNumber *barTintColor = options.barTintColor().has_value() ? [NSNumber numberWithDouble:options.barTintColor().value()] : nil;
  NSNumber *controlTintColor = options.controlTintColor().has_value() ? [NSNumber numberWithDouble:options.controlTintColor().value()] : nil;

#else
RCT_EXPORT_METHOD(open:(NSString *)url
                  options:(NSDictionary * _Nonnull)options
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {

  NSString *animationType = [options valueForKey:@"animationType"];
  NSString *dismissButtonStyle = [options valueForKey:@"dismissButtonStyle"];
  NSNumber *barTintColor = [options valueForKey:@"barTintColor"];
  NSNumber *controlTintColor = [options valueForKey:@"controlTintColor"];

#endif
  if (_safariVC != nil) {
    return reject(@"swan_browser_visible", @"An instance of the swan browser is already visible", nil);
  }

  @try {
    SFSafariViewControllerConfiguration *config = [SFSafariViewControllerConfiguration new];
    [config setBarCollapsingEnabled:false];
    [config setEntersReaderIfAvailable:false];

    _safariVC = [[SFSafariViewController alloc] initWithURL:[[NSURL alloc] initWithString:url] configuration:config];

    if (dismissButtonStyle == nil || [dismissButtonStyle isEqualToString:@"close"]) {
      [_safariVC setDismissButtonStyle:SFSafariViewControllerDismissButtonStyleClose];
    } else if ([dismissButtonStyle isEqualToString:@"cancel"]) {
      [_safariVC setDismissButtonStyle:SFSafariViewControllerDismissButtonStyleCancel];
    } else if ([dismissButtonStyle isEqualToString:@"done"]) {
      [_safariVC setDismissButtonStyle:SFSafariViewControllerDismissButtonStyleDone];
    }

    if (barTintColor != nil) {
      [_safariVC setPreferredBarTintColor:[RCTConvert UIColor:barTintColor]];
    }
    if (controlTintColor != nil) {
      [_safariVC setPreferredControlTintColor:[RCTConvert UIColor:controlTintColor]];
    }

    if (animationType != nil && [animationType isEqualToString:@"fade"]) {
      [_safariVC setModalPresentationStyle:UIModalPresentationOverFullScreen];
      [_safariVC setModalTransitionStyle:UIModalTransitionStyleCrossDissolve];
    } else {
      [_safariVC setModalPresentationStyle:UIModalPresentationPageSheet];
    }

    [RCTPresentedViewController() presentViewController:_safariVC animated:true completion:nil];

    _safariVC.delegate = self;
    _safariVC.presentationController.delegate = self;

    resolve(nil);
  } @catch (NSException *exception) {
    _safariVC = nil;
    reject(exception.name, exception.reason, nil);
  }
}

#ifdef RCT_NEW_ARCH_ENABLED
- (void)close {
#else
RCT_EXPORT_METHOD(close) {
#endif
  if (_safariVC != nil) {
    [_safariVC dismissViewControllerAnimated:true completion:^{
      [self handleOnClose];
    }];
  }
}

// Presentation anchor for ASWebAuthenticationSession
- (ASPresentationAnchor)presentationAnchorForWebAuthenticationSession:(ASWebAuthenticationSession *)session {
  return RCTKeyWindow();
}

// Open auth session for OAuth flows - uses ASWebAuthenticationSession
#ifdef RCT_NEW_ARCH_ENABLED
- (void)openAuthSession:(NSString *)url
            redirectUrl:(NSString *)redirectUrl
                options:(JS::NativeRNSwanBrowser::AuthSessionOptions &)options
                resolve:(RCTPromiseResolveBlock)resolve
                 reject:(RCTPromiseRejectBlock)reject {

  BOOL prefersEphemeralSession = options.prefersEphemeralSession().value_or(false);

#else
RCT_EXPORT_METHOD(openAuthSession:(NSString *)url
                      redirectUrl:(NSString *)redirectUrl
                          options:(NSDictionary * _Nonnull)options
                          resolve:(RCTPromiseResolveBlock)resolve
                           reject:(RCTPromiseRejectBlock)reject) {

  BOOL prefersEphemeralSession = [[options valueForKey:@"prefersEphemeralSession"] boolValue];

#endif
  if (_authSession != nil) {
    return reject(@"auth_session_in_progress", @"An auth session is already in progress", nil);
  }

  NSString *scheme = [[NSURL URLWithString:redirectUrl] scheme];

  _authSession = [[ASWebAuthenticationSession alloc]
    initWithURL:[NSURL URLWithString:url]
    callbackURLScheme:scheme
    completionHandler:^(NSURL *callbackURL, NSError *error) {
      self->_authSession = nil;

      if (callbackURL != nil) {
        resolve(@{@"type": @"success", @"url": callbackURL.absoluteString});
      } else if (error != nil && error.code == ASWebAuthenticationSessionErrorCodeCanceledLogin) {
        resolve(@{@"type": @"cancel"});
      } else if (error != nil) {
        reject(@"auth_session_error", error.localizedDescription, error);
      } else {
        reject(@"auth_session_error", @"Unknown error", nil);
      }
    }];

  _authSession.prefersEphemeralWebBrowserSession = prefersEphemeralSession;
  _authSession.presentationContextProvider = self;

  if (![_authSession start]) {
    _authSession = nil;
    reject(@"auth_session_failed", @"Failed to start auth session", nil);
  }
}

// Cancel any in-progress auth session
#ifdef RCT_NEW_ARCH_ENABLED
- (void)cancelAuthSession {
#else
RCT_EXPORT_METHOD(cancelAuthSession) {
#endif
  if (_authSession != nil) {
    [_authSession cancel];
    _authSession = nil;
  }
}

@end
