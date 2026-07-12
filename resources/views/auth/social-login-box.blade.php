@if(getSetting('social.facebook_client_id') || getSetting('social.twitter_client_id') || getSetting('social.google_client_id') || config('services.webbycloud.client_id'))
    <div class="social-login-links">

        <div class="strike mt-2">
            <span>{{__("Or use social")}}</span>
        </div>

        <div class="mt-4">
            @if(getSetting('social.facebook_client_id'))
                <div class="d-flex justify-content-center">
                    <a href="{{url('',['socialAuth','facebook'])}}" rel="nofollow" class="btn btn-block btn-outline-primary btn-round">
                        <div class="d-flex align-items-center justify-content-center">
                            <div class="col-4 d-flex flex-row-reverse pr-0">
                                <img src="{{asset('/img/logos/facebook-logo.svg')}}" class="social-media-icon"/>
                            </div>
                            <div class="col-8 d-flex align-items-center flex-row ">
                                {{__("Sign in with")}} {{__("Facebook")}}
                            </div>
                        </div>
                    </a>
                </div>
            @endif

            @if(getSetting('social.twitter_client_id'))
                <div class="d-flex justify-content-center">
                    <a href="{{url('',['socialAuth','twitter'])}}" rel="nofollow" class="btn btn-block btn-outline-primary btn-round">
                        <div class="d-flex align-items-center justify-content-center">
                            <div class="col-4 d-flex flex-row-reverse pr-0">
                                <img src="{{asset('/img/logos/x-logo.svg')}}" class="social-media-icon"/>
                            </div>
                            <div class="col-8 d-flex align-items-center flex-row ">
                                {{__("Sign in with")}} {{__("X")}}
                            </div>
                        </div>
                    </a>
                </div>
            @endif

            @if(getSetting('social.google_client_id'))
                <div class="d-flex justify-content-center">
                    <a href="{{url('',['socialAuth','google'])}}" rel="nofollow" class="btn btn-block btn-outline-primary btn-round">
                        <div class="d-flex align-items-center justify-content-center">
                            <div class="col-4 d-flex flex-row-reverse pr-0">
                                <img src="{{asset('/img/logos/google-logo.svg')}}" class="social-media-icon"/>
                            </div>
                            <div class="col-8 d-flex align-items-center flex-row ">
                                {{__("Sign in with")}} {{__("Google")}}
                            </div>
                        </div>
                    </a>
                </div>
            @endif

            @if(config('services.webbycloud.client_id'))
                <div class="d-flex justify-content-center">
                    <a href="{{route('webbycloud.login')}}" rel="nofollow" class="btn btn-block btn-outline-primary btn-round">
                        <div class="d-flex align-items-center justify-content-center">
                            <div class="col-4 d-flex flex-row-reverse pr-0">
                                <svg class="social-media-icon" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z"/>
                                </svg>
                            </div>
                            <div class="col-8 d-flex align-items-center flex-row ">
                                {{__("Connect with")}} {{__("WebbyCloud")}}
                            </div>
                        </div>
                    </a>
                </div>
            @endif
        </div>
    </div>

@endif
