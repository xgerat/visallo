define([
    'create-react-class',
    'prop-types'
], function(
    createReactClass,
    PropTypes) {
    'use strict';

    const VisibilityEditor = createReactClass({
        propTypes: {
            //TODO
        },

        getDefaultProps() {
            return { value: '', placeholder: i18n('visibility.label') }
        },

        getInitialState() {
            return { value: this.props.value, valid: true }
        },

        componentWillReceiveProps({ value }) {
            if (value !== this.state.value) {
                this.setState({ value, valid: this.checkValid(value) })
            }
        },

        render() {
            const { placeholder } = this.props;
            const { value, valid } = this.state;

            return (
                <input
                    ref={r => { this.node = r }}
                    type="text"
                    onChange={this.onChange}
                    value={value}
                    placeholder={placeholder}
                    className={valid ? '' : 'invalid'} />
            )
        },

        onChange(event) {
            const value = event.target.value;
            const valid = this.checkValid(value);

            this.setState({ value, valid })
            this.props.visibilitychange({ value, valid })
        },

        clear() {
            this.setState({ value: '', valid: true });
        },

        checkValid(value) {
            const authorizations = this.props.authorizations;

            return Boolean(!value.length || value in authorizations);
        }
    });

    return VisibilityEditor;
});
