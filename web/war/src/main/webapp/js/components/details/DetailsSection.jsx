define([
    'react'
], function (React) {
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
             * optional method to render additional elements next to the title element
             */
            renderAdditionalTitleElements: React.PropTypes.func,

            /**
             * event called when the section is toggled.
             */
            onToggle: React.PropTypes.func.isRequired,

            /**
             * true, if expanded
             */
            expanded: React.PropTypes.bool.isRequired
        },

        toggleExpanded() {
            this.props.onToggle();
        },

        handleTitleClick() {
            this.toggleExpanded();
        },

        render() {
            return (<section
                className={`${this.props.className || ''} collapsible ${this.props.expanded ? 'expanded' : ''}`}>
                <h1 onClick={this.handleTitleClick}>
                    <strong>{this.props.title}</strong>
                    {this.props.renderAdditionalTitleElements ? this.props.renderAdditionalTitleElements() : null}
                </h1>
                {this.props.expanded ? this.props.children : null}
            </section>);
        }
    });
});
