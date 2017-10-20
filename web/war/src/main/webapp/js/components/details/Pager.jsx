define([
    'react',
    'util/formatters'
], function (React, F) {
    'use strict';

    return React.createClass({
        propTypes: {
            /**
             * Current page number
             */
            page: React.PropTypes.number.isRequired,

            /**
             * Number of pages
             */
            pageCount: React.PropTypes.number.isRequired,

            /**
             * Event when previous page is clicked
             */
            onPreviousClick: React.PropTypes.func.isRequired,

            /**
             * Event when next page is clicked
             */
            onNextClick: React.PropTypes.func.isRequired,

            /**
             * true, to display loading indicator
             */
            loading: React.PropTypes.bool
        },

        handlePreviousClick() {
            if (this.props.page > 0) {
                this.props.onPreviousClick();
            }
        },

        handleNextClick() {
            if (this.props.page < this.props.pageCount) {
                this.props.onNextClick();
            }
        },

        render() {
            return (<p className="paging">
                {i18n('detail.paging',
                    F.number.pretty(this.props.page),
                    F.number.pretty(this.props.pageCount)
                )}
                {this.props.loading ? (
                    <img src="../../img/loading.gif" style={{position: 'absolute'}} alt={i18n('detail.paging.loading')}
                         width="16"
                         height="16"/>) : null}
                <button className="previous" disabled={this.props.page <= 1} onClick={this.handlePreviousClick}/>
                <button className="next" disabled={this.props.page >= this.props.pageCount}
                        onClick={this.handleNextClick}/>
            </p>);
        }
    });
});
