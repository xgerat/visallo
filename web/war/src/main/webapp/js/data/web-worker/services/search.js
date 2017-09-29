define(['../util/ajax'], function(ajax) {
    'use strict';

    var api = {

        all: function(urlFilter) {
            var visalloFilter = /^\/(?:vertex|element|edge)\/search$/;
            return ajax('GET', '/search/all')
                .then(function(result) {
                    return _.chain(result.searches)
                        .filter(function(search) {
                            if (urlFilter) {
                                if (visalloFilter.test(urlFilter)) {
                                    return visalloFilter.test(search.url);
                                }
                                return search.url === urlFilter;
                            }
                            return true;
                        })
                        .sortBy(function(search) {
                            return search.name.toLowerCase();
                        })
                        .value();
                })
        },

        global: function(urlFilter) {
            var visalloFilter = /^\/(?:vertex|element|edge)\/search$/;
            return ajax('GET', '/search/global')
                .then(function(result) {
                    return _.chain(result.searches)
                        .filter(function(search) {
                            if (urlFilter) {
                                if (visalloFilter.test(urlFilter)) {
                                    return visalloFilter.test(search.url);
                                }
                                return search.url === urlFilter;
                            }
                            return true;
                        })
                        .sortBy(function(search) {
                            return search.name.toLowerCase();
                        })
                        .value();
                })
        },

        user: function(urlFilter) {
            var visalloFilter = /^\/(?:vertex|element|edge)\/search$/;
            return ajax('GET', '/search/user')
                .then(function(result) {
                    return _.chain(result.searches)
                        .filter(function(search) {
                            if (urlFilter) {
                                if (visalloFilter.test(urlFilter)) {
                                    return visalloFilter.test(search.url);
                                }
                                return search.url === urlFilter;
                            }
                            return true;
                        })
                        .sortBy(function(search) {
                            return search.name.toLowerCase();
                        })
                        .value();
                })
        },

        favorite: function(urlFilter) {
            var visalloFilter = /^\/(?:vertex|element|edge)\/search$/;
            return ajax('GET', '/search/favorite')
                .then(function(result) {
                    return _.chain(result.searches)
                        .filter(function(search) {
                            if (urlFilter) {
                                if (visalloFilter.test(urlFilter)) {
                                    return visalloFilter.test(search.url);
                                }
                                return search.url === urlFilter;
                            }
                            return true;
                        })
                        .sortBy(function(search) {
                            return search.name.toLowerCase();
                        })
                        .value();
                })
        },

        save: function(query) {
            var toFix = [],
                params = query.parameters;

            if (params) {
                _.each(params, function(value, name) {
                    if (_.isArray(value)) {
                        toFix.push(name);
                    }
                });
                toFix.forEach(function(name) {
                    if (!(/\[\]$/).test(name)) {
                        params[name + '[]'] = params[name];
                        delete params[name];
                    }
                });
            }
            return ajax('POST', '/search/save', query);
        },

        delete: function(queryId) {
            return ajax('DELETE->HTML', '/search', {
                id: queryId
            });
        },

        run: function(queryId, otherParams) {
            return ajax('GET', '/search/run', _.extend({}, otherParams || {}, {
                id: queryId
            }));
        },

        favoriteSave: function(searchId) {
            return ajax('POST', '/search/favorite/save', {
                id: searchId
            });
        },

        unfavorite: function(searchId) {
            return ajax('DELETE->HTML', '/search/unfavorite', {
                id: searchId
            })
        },

        get: function(searchId) {
            return ajax('GET', '/search/get', {
                id: searchId
            })
        }

    };

    return api;
});
