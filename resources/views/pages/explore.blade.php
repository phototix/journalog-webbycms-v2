@extends('layouts.user-no-nav')
@section('page_title', __('Explore'))

@section('styles')
    {!!
    Minify::stylesheet(array_merge([
            '/css/pages/feed.css',
            '/css/pages/search.css',
            '/css/pages/lists.css',
         ],$additionalAssets['css']))->withFullUrl()
    !!}
@stop

@section('scripts')
    {!!
        Minify::javascript(array_merge([
            '/js/UsersPaginator.js',
            '/js/SuggestionsSlider.js',
            '/js/pages/lists.js',
            '/libs/swiper/swiper-bundle.min.js',
             ],$additionalAssets['js']))->withFullUrl()
    !!}
    <script>
        UsersPaginator.init('.users-wrapper');
        UsersPaginator.initScrollLoad();
    </script>
@stop

@section('content')
    <div class="container">
        <div class="row">
            <div class="col-12 col-sm-12 col-lg-8 col-md-7 second p-0">
                <div class="d-flex d-md-none px-3 py-3 feed-mobile-search neutral-bg fixed-top-m">
                    @include('elements.search-box')
                </div>

                <div class="m-pt-70"></div>

                <div>
                    @include('elements.message-alert',['classes'=>'pt-4 pb-4 px-2'])
                    <div class="users-wrapper mt-0 pt-4">
                        @foreach($users as $user)
                            @include('elements.feed.suggestion-card',[
                                'profile' => $user,
                                'isListMode' => true,
                                'isListManageable' => false,
                                'classes' => 'mb-3 w-100',
                            ])
                        @endforeach
                    </div>
                    @include('elements.feed.posts-loading-spinner')
                </div>
            </div>

            <div class="col-12 col-sm-12 col-md-5 col-lg-4 first border-left order-0 pt-4 pb-5 min-vh-100 suggestions-wrapper d-none d-md-block">
                <div class="feed-widgets">
                    @if(!getSetting('feed.search_widget_hide'))
                        <div class="mb-3">
                            @include('elements.search-box')
                        </div>
                    @endif
                    @if(!getSetting('feed.hide_suggestions_slider'))
                        @include('elements.feed.suggestions-box',[
                             'id' => 'suggestions-box',
                             'profiles' => $suggestions,
                             'isMobile' => false,
                             'hideControls' => false,
                             'title' => __('Suggestions'),
                             'perPage' => (int)getSetting('feed.feed_suggestions_card_per_page'),
                        ])
                    @endif
                    @if(getSetting('code-and-ads.sidebar_ad_spot'))
                        <div class="mt-3">
                            {!! getSetting('code-and-ads.sidebar_ad_spot') !!}
                        </div>
                    @endif
                    @include('template.footer-feed')
                </div>
            </div>
        </div>
    </div>
@stop
