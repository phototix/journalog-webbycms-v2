/**
 * Gift system - handles gift modal, sending, and animations
 */
"use strict";

var Gift = (function () {
    var currentPostId = null;
    var currentRecipientId = null;
    var giftData = null;
    var isSending = false;

    function init() {
        loadGifts();
        bindEvents();
    }

    function loadGifts() {
        $.get('/gifts/list', function (response) {
            giftData = response.gifts;
            renderGifts('all', response.gifts);
            renderCategoryTabs(response.gifts);
            updateBalance(response.balance);
        });
    }

    function renderCategoryTabs(gifts) {
        var categories = Object.keys(gifts);
        $('.gift-category-tabs .nav-link').each(function () {
            var cat = $(this).text().trim();
            if (cat !== 'All') {
                if (!categories.includes(cat)) {
                    $(this).closest('li').hide();
                } else {
                    $(this).closest('li').show();
                }
            }
        });
    }

    function renderGifts(category, gifts) {
        $('.gift-grid').each(function () {
            var gridCategory = $(this).data('category');
            var items = [];

            if (gridCategory === 'all') {
                Object.values(gifts).forEach(function (catGifts) {
                    items = items.concat(catGifts);
                });
            } else if (gifts[gridCategory]) {
                items = gifts[gridCategory];
            }

            var html = '';
            items.forEach(function (gift) {
                html += '<div class="col-4 col-md-3 mb-3">';
                html += '  <div class="gift-card card text-center p-2 h-100" data-gift-id="' + gift.id + '" data-credits="' + gift.credits + '" data-gif-effect="' + (gift.gif_effect || '') + '">';
                html += '    <div class="gift-card-icon my-2">';
                html += '      <ion-icon name="' + gift.icon + '" size="large"></ion-icon>';
                html += '    </div>';
                html += '    <div class="gift-card-name small font-weight-bold">' + gift.name + '</div>';
                html += '    <div class="gift-card-credits text-muted small">' + gift.credits + ' credits</div>';
                html += '    <button class="btn btn-sm btn-primary mt-2 send-gift-action">' + trans('Send') + '</button>';
                html += '  </div>';
                html += '</div>';
            });

            if (!items.length) {
                html = '<div class="col-12 text-center text-muted py-4">' + trans('No gifts in this category') + '</div>';
            }

            $(this).html(html);
        });
    }

    function bindEvents() {
        $(document).on('click', '.send-gift-action', function () {
            var card = $(this).closest('.gift-card');
            var giftId = card.data('gift-id');
            var credits = card.data('credits');
            var gifEffect = card.data('gif-effect');

            sendGift(giftId, credits, gifEffect);
        });

        $('#gift-modal').on('show.bs.modal', function (e) {
            var btn = $(e.relatedTarget);
            currentPostId = btn.data('post-id');
            currentRecipientId = btn.data('recipient-id');
        });

        $('#gift-modal').on('shown.bs.modal', function () {
            if (giftData) {
                renderGifts('all', giftData);
            }
        });

        $('.gift-category-tabs a').on('shown.bs.tab', function () {
            if (giftData) {
                renderGifts('all', giftData);
            }
        });
    }

    function sendGift(giftId, credits, gifEffect) {
        if (isSending) return;
        isSending = true;

        var btn = $('.send-gift-action[data-gift-id="' + giftId + '"]');
        btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm"></span>');

        var idempotencyKey = generateIdempotencyKey();

        $.ajax({
            url: '/gifts/send',
            method: 'POST',
            data: {
                gift_id: giftId,
                post_id: currentPostId,
                idempotency_key: idempotencyKey,
                _token: $('meta[name="csrf-token"]').attr('content')
            },
            success: function (response) {
                if (response._idempotent) {
                    return;
                }
                $('#gift-modal').modal('hide');
                updateBalance(response.balance);
                updatePostGifts(response.post_gifts);

                if (response.gift && response.gift.gif_effect) {
                    showGiftEffect(response.gift);
                }

                if (typeof launchToast !== 'undefined') {
                    launchToast(response.message, 'success');
                }
            },
            error: function (xhr) {
                var err = xhr.responseJSON;
                var msg = err && err.error ? err.error : trans('Something went wrong');

                if (typeof launchToast !== 'undefined') {
                    launchToast(msg, 'danger');
                }

                if (err && err.balance !== undefined) {
                    updateBalance(err.balance);
                }
            },
            complete: function () {
                isSending = false;
                btn.prop('disabled', false).text(trans('Send'));
            }
        });
    }

    function generateIdempotencyKey() {
        var timestamp = Date.now().toString(36);
        var random = Math.random().toString(36).substring(2, 10);
        return timestamp + '-' + random;
    }

    function updateBalance(balance) {
        var formatted = getWebsiteFormattedAmount(balance);
        $('.gift-wallet-balance').text(formatted);
        $('.available-credit').text('(' + formatted + ')');
    }

    function updatePostGifts(postGifts) {
        var container = $('.post-gifts-container[data-post-id="' + currentPostId + '"]');
        if (!container.length) return;

        container.removeClass('d-none').empty();

        postGifts.forEach(function (pg) {
            var item = '<div class="post-gift-item d-flex align-items-center mr-3 mb-1" data-gift-id="' + pg.gift_id + '">';
            item += '  <ion-icon name="' + pg.gift.icon + '" class="gift-icon mr-1"></ion-icon>';
            item += '  <span class="gift-count font-weight-bold">' + pg.count + '</span>';
            item += '</div>';
            container.append(item);
        });

        var totalCount = postGifts.reduce(function (sum, pg) { return sum + pg.count; }, 0);
        $('.post-gifts-label-count').text(totalCount);
    }

    function showGiftEffect(gift) {
        var overlay = $('#gift-effect-overlay');
        if (!overlay.length) return;

        overlay.find('.gift-effect-gif').attr('src', gift.gif_effect);
        overlay.find('.gift-effect-text').text(gift.name + '!');
        overlay.removeClass('d-none').fadeIn(300);

        setTimeout(function () {
            overlay.fadeOut(300, function () {
                overlay.addClass('d-none');
            });
        }, 3000);
    }

    function getWebsiteFormattedAmount(amount) {
        if (typeof window.getWebsiteFormattedAmount === 'function') {
            return window.getWebsiteFormattedAmount(amount);
        }
        return '$' + parseFloat(amount).toFixed(2);
    }

    return {
        init: init,
        loadGifts: loadGifts,
        sendGift: sendGift,
    };
})();

$(function () {
    Gift.init();
});
