/**
 * Paginator component - used for user listing pagination
 */
"use strict";
/* global paginatorConfig */

var UsersPaginator = {

    isFetching: false,
    nextPageUrl: '',
    prevPageUrl: '',
    currentPage: null,
    container: '',
    method: 'GET',

    /**
     * Initiates the component
     * @param route
     * @param container
     * @param method
     */
    init: function (container, method='GET') {
        UsersPaginator.nextPageUrl = paginatorConfig.next_page_url;
        UsersPaginator.prevPageUrl = paginatorConfig.prev_page_url;
        UsersPaginator.currentPage = paginatorConfig.current_page;
        UsersPaginator.container = container;
        UsersPaginator.method = method;
    },

    /**
     * Loads (new) up paginated results
     * @param direction
     */
    loadResults: function (direction='next') {
        if(UsersPaginator.isFetching === true || !UsersPaginator.nextPageUrl){
            return false;
        }
        UsersPaginator.isFetching = true;
        let url = UsersPaginator.nextPageUrl;
        if(direction === 'prev'){
            url = UsersPaginator.prevPageUrl;
        }
        UsersPaginator.toggleLoadingIndicator(true);
        $.ajax({
            type: UsersPaginator.method,
            url: url,
            dataType: 'json',
            success: function(result) {
                if(result.success){

                    if(result.data.hasMore === false){
                        UsersPaginator.unbindPaginator();
                    }
                    if(direction !== 'prev'){
                        UsersPaginator.nextPageUrl = result.data.next_page_url;
                    }
                    else{
                        UsersPaginator.prevPageUrl = result.data.prev_page_url;
                        $('.reverse-paginate-btn').find('button').removeClass('disabled');
                    }

                    if(result.data.prev_page_url === null){
                        $('.reverse-paginate-btn').fadeOut("fast", function() {});
                    }

                    UsersPaginator.appendResults(result.data.users, direction);
                    UsersPaginator.isFetching = false;
                }
                else{
                    UsersPaginator.isFetching = false;
                }
                UsersPaginator.toggleLoadingIndicator(false);
            }
        });
    },

    /**
     * Toggles the loading indicator
     * @param loading
     */
    toggleLoadingIndicator: function(loading = false){
        if(loading === true){
            $('.posts-loading-indicator .spinner').removeClass('d-none');
        }
        else{
            $('.posts-loading-indicator .spinner').addClass('d-none');
        }
    },

    /**
     * Appends new users to the container
     * @param users
     * @param direction
     */
    appendResults: function(users, direction = 'next'){
        let htmlOut = [];
        $.map(users, function (user) {
            htmlOut.push(user.html);
        });

        if(direction === 'next'){
            $(UsersPaginator.container).append(htmlOut.join('')).fadeIn('slow');
        }else{
            $(UsersPaginator.container).prepend(htmlOut.join('')).fadeIn('slow');
        }
    },

    /**
     * Initiates infinite scrolling
     */
    initScrollLoad: function(){
        window.onscroll = function() {
            if (((window.innerHeight + window.scrollY + 2) * window.devicePixelRatio.toFixed(2)) >= document.body.offsetHeight * window.devicePixelRatio.toFixed(2)) {
                UsersPaginator.loadResults();
            }
        };
    },

    /**
     * Unbinds the paginator infinite scrolling behaviour
     */
    unbindPaginator: function () {
        UsersPaginator.nextPageUrl = '';
        window.onscroll = function() {};
    },

};
