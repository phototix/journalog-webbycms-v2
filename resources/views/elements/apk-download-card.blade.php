<div class="card feed-widget mb-3">
    <div class="card-body text-center">
        <div class="mb-2">@include('elements.icon',['icon'=>'logo-android','variant'=>'large'])</div>
        <h6 class="font-weight-bold mb-2">{{ __('Android App') }}</h6>
        <a href="{{ url('/apk/latest') . '?t=' . time() }}" class="btn btn-sm btn-primary btn-round d-inline-flex align-items-center">
            @include('elements.icon',['icon'=>'download-outline','centered'=>false,'classes'=>'mr-1'])
            <span>{{ __('Download') }}</span>
        </a>
    </div>
</div>
