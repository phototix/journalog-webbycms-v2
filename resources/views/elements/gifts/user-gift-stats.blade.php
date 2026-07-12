<div class="card mt-3 profile-gift-stats-card">
    <div class="card-header">
        <h5 class="mb-0">{{__('Gifts Received')}}</h5>
    </div>
    <div class="card-body">
        <div class="profile-gift-stats" data-user-id="{{$user->id}}">
            <div class="d-flex justify-content-around text-center mb-3">
                <div>
                    <div class="font-weight-bold h4 mb-0 total-gifts-count">{{$totalGifts ?? 0}}</div>
                    <small class="text-muted">{{__('Total Gifts')}}</small>
                </div>
                <div>
                    <div class="font-weight-bold h4 mb-0 total-credits-value">{{\App\Providers\SettingsServiceProvider::getWebsiteFormattedAmount($totalCredits ?? 0)}}</div>
                    <small class="text-muted">{{__('Total Value')}}</small>
                </div>
            </div>
            <div class="gift-stats-list">
                @if(isset($giftStats) && $giftStats->count() > 0)
                    @foreach($giftStats as $stat)
                        <div class="stat-item d-flex align-items-center py-2 border-bottom">
                            @include('elements.icon',['icon'=>$stat->gift->icon, 'variant'=>'small', 'centered'=>false, 'classes' => 'gift-icon mr-2'])
                            <span class="flex-grow-1">{{$stat->gift->name}}</span>
                            <span class="font-weight-bold">{{$stat->count}}x</span>
                        </div>
                    @endforeach
                @else
                    <p class="text-muted text-center mb-0">{{__('No gifts received yet.')}}</p>
                @endif
            </div>
        </div>
    </div>
</div>
