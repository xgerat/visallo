
define([
    'flight/lib/component',
    'util/popovers/withPopover',
    'util/withDataRequest',
    'hbs!./list'
], function(
    defineComponent,
    withPopover,
    withDataRequest,
    template) {
    'use strict';

    var SCOPES = {
        GLOBAL: 'Global',
        FAVORITE: 'Favorite'
    };

    var SEARCH_TYPES = ['Favorite', 'User', 'Global'];

    return defineComponent(SavedSearches, withPopover, withDataRequest);

    function normalizeParameters(params) {
        return _.chain(params)
            .map(function(value, key) {
                return [
                    key.replace(/\[\]$/, ''),
                    value
                ];
            })
            .object()
            .value();
    }

    function queryParametersChanged(param1, param2) {
        return !_.isEqual(
            normalizeParameters(param1),
            normalizeParameters(param2)
        );
    }

    function SavedSearches() {

        this.defaultAttrs({
            favoriteSelector: '.rating',
            listSelector: 'li',
            saveSelector: '.form button',
            nameInputSelector: '.form input.name',
            globalSearchSelector: '.form .global-search',
            globalInputSelector: '.form .global-search input',
            deleteSelector: 'ul .btn-danger',
            savedSearchListTypeSelector: '.saved-search',
            savedSearchTypeSelector: '.saved-search-type',
            savedSearchListSelector: '.saved-search-list'
        });

        this.before('initialize', function(node, config) {
            config.template = '/search/save/template';
            config.canSaveGlobal = visalloData.currentUser.privileges.indexOf('SEARCH_SAVE_GLOBAL') > -1;
            config.maxHeight = $(window).height() / 2;
            config.name = config.update && config.update.name || '';
            config.updatingGlobal = config.update && config.update.scope === SCOPES.GLOBAL;
            config.text = i18n('search.savedsearches.button.' + (config.update ? 'update' : 'create'));
            config.teardownOnTap = true;
            config.canAddOrUpdate = _.isUndefined(config.update) || !config.updatingGlobal ||
                (config.updatingGlobal && config.canSaveGlobal);
            config.types = SEARCH_TYPES.map(function(type, i) {
                return {
                    name: type,
                    displayName: {
                        Favorite: i18n('search.savedsearches.button.favorite'),
                        User: i18n('search.savedsearches.button.user'),
                        Global: i18n('search.savedsearches.button.global')
                    }[type],
                    selected: i === 0
                }
            });

            if (config.list.length > 0) {
                config.list = config.list.map(function(item) {
                    var isGlobal = item.scope === SCOPES.GLOBAL,
                        canDelete = true;
                    if (isGlobal) {
                        canDelete = config.conSaveGlobal;
                    }
                    var tooltip = i18n('search.savedsearches.' + (item.favorited ? 'delete' : 'add') + '.favorite');
                    return _.extend({}, item, {
                        isGlobal: isGlobal,
                        canDelete: canDelete && item.scope !== SCOPES.FAVORITE,
                        tooltip: tooltip
                    })
                })
            }

            this.after('setupWithTemplate', function() {
                this.on(this.popover, 'click', {
                    favoriteSelector: this.onFavoriteClick,
                    deleteSelector: this.onDelete,
                    listSelector: this.onClick,
                    saveSelector: this.onSave,
                    savedSearchListTypeSelector: this.onSavedSearchListTypeClick
                });

                this.attr.list = config.list;

                var $savedSearchList = this.popover.find(this.attr.savedSearchListSelector);
                $savedSearchList.append(template({list: this.attr.list}));

                this.on(this.popover, 'keyup change', {
                    nameInputSelector: this.onChange,
                    globalInputSelector: this.onChange
                });

                this.validate();
                this.positionDialog();
            });
        });

        this.after('initialize', function() {
            this.on('setCurrentSearchForSaving', this.onSetCurrentSearchForSaving);
        });

        this.onSetCurrentSearchForSaving = function(event, data) {
            this.attr.query = data;
            this.validate();
        };

        this.onChange = function(event) {
            if (this.attr.update) {
                var $button = this.popover.find('.form button'),
                    query = this.getQueryForSaving();

                $button.text(
                    ('id' in query) ?
                    'Update' : 'Create'
                );
            }

            if (this.validate() && event.type === 'keyup' && event.which === 13) {
                this.save();
            }
        };

        this.validate = function() {
            var $input = this.popover.find(this.attr.nameInputSelector),
                $button = this.popover.find(this.attr.saveSelector),
                $global = this.popover.find(this.attr.globalSearchSelector),
                query = this.getQueryForSaving(),
                noParameters = _.isEmpty(query.parameters);

            $button.prop('disabled', !query.name || noParameters);
            $input.prop('disabled', noParameters || !query.url);
            $global.toggle(!(noParameters || !query.url));

            return query.name && query.url && !noParameters;
        };

        this.getQueryForSaving = function() {
            var $nameInput = this.popover.find(this.attr.nameInputSelector),
                $globalInput = this.popover.find(this.attr.globalInputSelector),
                query = {
                    name: $nameInput.val().trim(),
                    global: $globalInput.is(':checked'),
                    url: this.attr.query && this.attr.query.url,
                    parameters: this.attr.query && this.attr.query.parameters
                };

            if (this.attr.update && query.parameters) {
                var nameChanged = this.attr.update.name !== query.name,
                    queryChanged = queryParametersChanged(this.attr.update.parameters, query.parameters);

                if ((nameChanged && !queryChanged) || (!nameChanged && queryChanged) ||
                   (!nameChanged && !queryChanged)) {
                    query.id = this.attr.update.id;
                }
            }

            return query;
        };

        this.onSave = function(event) {
            this.save();
        };

        this.save = function() {
            var self = this,
                $button = this.popover.find(this.attr.saveSelector).addClass('loading'),
                query = this.getQueryForSaving();

            this.dataRequest('search', 'save', query)
                .then(function() {
                    self.teardown();
                })
                .finally(function() {
                    $button.removeClass('loading');
                })
        };

        this.onDelete = function(event) {
            var self = this,
                $li = $(event.target).closest('li'),
                index = $li.index(),
                query = this.attr.list[index],
                $button = $(event.target).addClass('loading');

            $li.addClass('loading');
            event.stopPropagation();

            this.dataRequest('search', 'delete', query.id)
                .then(function() {
                    if ($li.siblings().length === 0) {
                        $li.closest('ul').html(
                            $('<li class="empty">No Saved Searches Found</li>')
                        );
                    } else $li.remove();

                    self.attr.list.splice(index, 1);

                    if (self.attr.update && self.attr.update.id === query.id) {
                        self.popover.find(self.attr.nameInputSelector).val('');
                        self.popover.find(self.attr.saveSelector).text('Create');
                        self.attr.update = null;
                        self.validate();
                    }
                })
                .finally(function() {
                    $button.removeClass('loading');
                    $li.removeClass('loading');
                })
        };

        this.onClick = function(event) {
            var query = this.attr.list[$(event.target).closest('li').index()];

            this.trigger('savedQuerySelected', {
                query: query
            });

            this.teardown();
        };

        this.onFavoriteClick = function(event) {
            event.stopPropagation();

            var self = this,
                $target = $(event.currentTarget),
                isFavorited = $target.hasClass('favorited'),
                $li = $target.closest('li'),
                index = $li.index(),
                searchId = this.attr.list[index].id;

                // user wants to favorite search
                if (!isFavorited) {
                    this.dataRequest('search', 'favoriteSave', searchId)
                        .then(function() {
                            self.attr.list[index].favorite = true;
                        }).finally(function() {
                            $target.addClass('favorited')
                                .removeClass('not-favorited')
                                .attr('title', i18n('search.savedsearches.delete.favorite'));
                        })
                } else if (isFavorited) {
                    // user wants to unfavorite search

                    this.dataRequest('search', 'unfavorite', searchId)
                        .then(function() {
                            self.attr.list[index].favorite = false;
                            var activeType = $('.saved-search').find('.active').data('type');
                            if (activeType === 'Favorite') {
                                if ($li.siblings().length === 0) {
                                    $li.closest('ul').html($('<li class="empty">No Saved Searches Found</li>'));
                                } else {
                                    $li.remove();
                                }
                                self.attr.list.splice(index, 1);
                            }
                        }).finally(function() {
                            $target.removeClass('favorited')
                                .addClass('not-favorited')
                                .attr('title', i18n('search.savedsearches.add.favorite'));
                        });
                }
        }

        this.onSavedSearchListTypeClick = function(event, data) {
            event.stopPropagation();
            this.switchListType($(event.target).blur().data('type'));
        }

        this.switchListType = function(listType) {
            var self = this,
                $field = this.popover.find(this.attr.savedSearchListSelector);
            $field.addClass('loading')
                .empty();
            this.popover.find('.saved-search-type.active').removeClass('active');
            this.popover.find('.saved-search-' + listType).addClass('active');
            this.dataRequest('search', listType.toLowerCase()).done(function(searches) {
                if (searches.length > 0) {
                    self.attr.list = searches.map(function(item) {
                        var isGlobal = item.scope === SCOPES.GLOBAL,
                            canDelete = true;
                        if (isGlobal) {
                            canDelete = visalloData.currentUser.privileges.indexOf('SEARCH_SAVE_GLOBAL') > -1;
                        }
                        var tooltip = i18n('search.savedsearches.' + (item.favorited ? 'delete' : 'add') + '.favorite');
                        return _.extend({}, item, {
                            isGlobal: isGlobal,
                            canDelete: canDelete && item.scope !== SCOPES.FAVORITE,
                            tooltip: tooltip
                        })
                    });
                } else {
                    self.attr.list = searches;
                }

                $field.append(template({
                    list: self.attr.list
                }));
            });
        }
    }
});
