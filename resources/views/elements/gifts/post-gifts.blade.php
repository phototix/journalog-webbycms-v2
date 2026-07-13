@php
    $postGifts = isset($gifts) ? $gifts : $post->gifts()->selectRaw('gift_id, count(*) as count')->groupBy('gift_id')->with('gift')->get();
@endphp
<div class="post-gifts-wrapper {{$postGifts->count() > 0 ? '' : 'd-none'}}" data-post-id="{{$post->id}}">
    <div class="post-gifts-container d-flex align-items-center flex-wrap">
        @foreach($postGifts as $pg)
            <div class="post-gift-item d-flex align-items-center mr-3 mb-1" data-gift-id="{{$pg->gift_id}}">
                @include('elements.icon',['icon'=>$pg->gift->icon, 'variant'=>'small', 'centered'=>false, 'classes' => 'gift-icon'])
                <span class="gift-count ml-1 font-weight-bold">{{$pg->count}}</span>
            </div>
        @endforeach
    </div>
</div>
