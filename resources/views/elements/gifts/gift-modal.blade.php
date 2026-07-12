<div class="modal fade" id="gift-modal" tabindex="-1" role="dialog" aria-labelledby="gift-modal-title" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="gift-modal-title">{{__('Send a Gift')}}</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="{{__('Close')}}">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <div class="gift-balance-box d-flex justify-content-between align-items-center mb-3 p-3 rounded">
                    <div>
                        <span class="text-muted">{{__('Your Balance')}}:</span>
                        <strong class="gift-wallet-balance ml-1">{{\App\Providers\SettingsServiceProvider::getWebsiteFormattedAmount(Auth::user()->wallet->total ?? 0)}}</strong>
                    </div>
                    <a href="{{route('my.settings', 'wallet')}}" class="btn btn-sm btn-primary">{{__('Add Credits')}}</a>
                </div>

                <ul class="nav nav-pills mb-3 gift-category-tabs" role="tablist">
                    <li class="nav-item" role="presentation">
                        <a class="nav-link active" id="gift-cat-all-tab" data-toggle="pill" href="#gift-cat-all" role="tab" aria-controls="gift-cat-all" aria-selected="true">{{__('All')}}</a>
                    </li>
                    <li class="nav-item" role="presentation">
                        <a class="nav-link" id="gift-cat-Romantic-tab" data-toggle="pill" href="#gift-cat-Romantic" role="tab" aria-controls="gift-cat-Romantic" aria-selected="false">{{__('Romantic')}}</a>
                    </li>
                    <li class="nav-item" role="presentation">
                        <a class="nav-link" id="gift-cat-Funny-tab" data-toggle="pill" href="#gift-cat-Funny" role="tab" aria-controls="gift-cat-Funny" aria-selected="false">{{__('Funny')}}</a>
                    </li>
                    <li class="nav-item" role="presentation">
                        <a class="nav-link" id="gift-cat-Premium-tab" data-toggle="pill" href="#gift-cat-Premium" role="tab" aria-controls="gift-cat-Premium" aria-selected="false">{{__('Premium')}}</a>
                    </li>
                    <li class="nav-item" role="presentation">
                        <a class="nav-link" id="gift-cat-Limited-Edition-tab" data-toggle="pill" href="#gift-cat-Limited-Edition" role="tab" aria-controls="gift-cat-Limited-Edition" aria-selected="false">{{__('Limited-Edition')}}</a>
                    </li>
                </ul>

                <div class="tab-content gift-tab-content">
                    <div class="tab-pane fade show active" id="gift-cat-all" role="tabpanel" aria-labelledby="gift-cat-all-tab">
                        <div class="row gift-grid" data-category="all"></div>
                    </div>
                    @foreach(['Romantic','Funny','Premium','Limited-Edition'] as $cat)
                        <div class="tab-pane fade" id="gift-cat-{{$cat}}" role="tabpanel" aria-labelledby="gift-cat-{{$cat}}-tab">
                            <div class="row gift-grid" data-category="{{$cat}}"></div>
                        </div>
                    @endforeach
                </div>

                <div class="gift-loading text-center py-4 d-none">
                    <div class="spinner-border text-primary" role="status">
                        <span class="sr-only">{{__('Loading...')}}</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
