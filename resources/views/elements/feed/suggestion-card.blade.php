<div class="suggestion-box {{isset($classes) ? $classes : ''}} card text-white border-0 rounded" data-memberuserid="{{$profile->id}}">
    <div style="background: url({{$profile->cover}});{{ isset($isListMode) && $isListMode ? 'height:210px !important' : '' }}" class="card-img suggestion-header-bg"></div>
    <div class="card-img-overlay p-0">
        <div class="h-100 w-100 p-0 m-0 position-absolute z-index-0">
            <div class="h-50">
            </div>
            <div class="h-50 w-100 half-bg d-flex rounded"></div>
        </div>
        <div class="card-text w-100 h-100 d-flex">

            <div class="d-flex align-items-center justify-content-center pl-3 pr-2 z-index-3">
                <img src="{{$profile->avatar}}" class="avatar rounded-circle"  />
            </div>

            <div class="w-100 z-index-3 text-truncate d-flex flex-column {{ isset($isListMode) && $isListMode ? 'h-100' : '' }}">
                <div class="{{ isset($isListMode) && $isListMode ? '' : 'h-50' }} d-flex flex-row-reverse pr-1">
                    @if(isset($isListMode) && ($isListManageable))
                        <span class="h-pill h-pill-accent rounded mt-1 suggestion-card-btn" data-toggle="tooltip" data-placement="bottom" title="" onclick="Lists.showListMemberRemoveModal({{$profile->id}})" data-original-title="{{__('Delete')}}">
                            @include('elements.icon',['icon'=>'trash-outline','variant'=>'medium'])
                        </span>
                    @endif
                </div>
                <div class="w-100 z-index-3 d-flex flex-column text-truncate pr-2 {{ isset($isListMode) && $isListMode ? 'mt-auto pb-2' : 'mt-1 h-50' }}">
                    <div class="m-0 h6 text-truncate"><a href="{{route('profile',['username'=>$profile->username])}}" class="text-white d-flex align-items-center">{{$profile->name}}
                        @if($profile->email_verified_at && $profile->birthdate && ($profile->verification && $profile->verification->status == 'verified'))
                            <span data-toggle="tooltip" data-placement="top" title="{{__('Verified user')}}">
                                @include('elements.icon',['icon'=>'checkmark-circle-outline','centered'=>true,'classes'=>'ml-1'])
                            </span>
                        @endif
                        </a>
                    </div>
                    <div class="m-0 text-truncate small"><span>@</span><a href="{{route('profile',['username'=>$profile->username])}}" class="text-white">{{$profile->username}}</a></div>
                    @if(isset($isListMode) && $isListMode && isset($profile->last_posted_at))
                        <div style="font-size:11px" class="text-white-50 mt-1 text-left w-100">
                            {{ __('Last Posted') }}: {{ $profile->last_posted_at ? \Carbon\Carbon::parse($profile->last_posted_at)->format('d M Y - h:i A') : __('Never') }}
                        </div>
                    @endif
                </div>
            </div>

        </div>
    </div>
</div>
