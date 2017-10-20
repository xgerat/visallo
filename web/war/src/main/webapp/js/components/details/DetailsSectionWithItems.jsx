define([
    'react',
    './DetailsSection'
], function (React, DetailsSection) {
    'use strict';

    return React.createClass({
        propTypes: {
            /**
             * Title to be displayed in the section header
             */
            title: React.PropTypes.string.isRequired,

            /**
             * Additional class names to apply to the section
             */
            className: React.PropTypes.string,

            /**
             * Value to display on the right of the section bar
             */
            badge: React.PropTypes.oneOfType([
                React.PropTypes.string,
                React.PropTypes.number
            ]),

            /**
             * Title to display on badge hover
             */
            badgeTitle: React.PropTypes.string,

            /**
             * event called when the search icon is clicked. If this property is not provided the search icon will
             * not be displayed.
             */
            onSearchClick: React.PropTypes.func,

            /**
             * Title to display on search icon hover
             */
            searchTitle: React.PropTypes.string,

            /**
             * event called when the section is toggled.
             */
            onToggle: React.PropTypes.func.isRequired,

            /**
             * true, if expanded
             */
            expanded: React.PropTypes.bool.isRequired
        },

        handleSearchClick(event) {
            event.stopPropagation();
            this.props.onSearchClick(event);
        },

        renderSearch() {
            if (!this.props.onSearchClick) {
                return null;
            }
            return (
                <s key="search" className="search-related" title={this.props.searchTitle || ''}
                   onClick={this.handleSearchClick}/>);
        },

        renderCount() {
            if (typeof this.props.badge === 'undefined' || this.props.badge === null) {
                return null;
            }
            return (<span key="counts" title={this.props.badgeTitle || ''} className="badge">{this.props.badge}</span>);
        },

        handleRenderAdditionalTitleElements() {
            return [
                this.renderSearch(),
                this.renderCount()
            ];
        },

        render() {
            return (<DetailsSection
                renderAdditionalTitleElements={this.handleRenderAdditionalTitleElements} {...this.props}/>);
        }
    });
});
