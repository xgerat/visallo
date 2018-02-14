define([
    'create-react-class',
    'prop-types'
], function(
    createReactClass,
    PropTypes) {
    'use strict';

    /**
     * @typedef org.visallo.visibility~Editor
     * @property {string} [value] The visibility source to prepopulate the editor
     * @property {string} [placeholder] The placeholder text to display when no
     * value
     * @property {string} [readonly] Show the form in read-only mode
     * @listens org.visallo.visibility#visibilityclear
     * @fires org.visallo.visibility#visibilitychange
     */
    const VisibilityEditor = createReactClass({
        propTypes: {
            onVisibilityChange: PropTypes.func
        },

        getDefaultProps() {
            return { value: '', placeholder: i18n('visibility.label'), onVisibilityChange: () => {} }
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
            this.props.onVisibilityChange({ value, valid })
        },

        onClear() {
            this.setState({ value: '', valid: true });
        },

        checkValid(value) {
            const authorizations = this.props.authorizations;

            return Boolean(!value.length || value in authorizations);
        }
    });

    return VisibilityEditor;
});
