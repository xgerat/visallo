define([
    'create-react-class',
    'prop-types',
    'react-redux',
    'data/web-worker/store/user/selectors',
    '../Attacher',
    '../RegistryInjectorHOC'
], function(
    createReactClass,
    PropTypes,
    redux,
    userSelectors,
    Attacher,
    RegistryInjectorHOC) {
    'use strict';

    const DEFAULT_EDITOR = 'components/visibility/default/VisibilityEditor';
    const apiWarnings = {};

    const VisibilityEditor = createReactClass({
        propTypes: {
            onVisibilityChange: PropTypes.func
            // TODO
        },

        getInitialState() {
            return {
                componentPath: DEFAULT_EDITOR
            }
        },

        componentDidMount() {
            $(this.node).on('visibilityClear', this.onVisibilityClear);
        },

        componentWillUnmount() {
            $(this.node).off('visibilityClear', this.onVisibilityClear);
        },

        componentWillReceiveProps(nextProps) {
            const extensions = _.values(nextProps.registry['org.visallo.visibility']).map(e => e.editorComponentPath);
            const componentPath = extensions[0] || DEFAULT_EDITOR;

            if (componentPath !== this.state.componentPath) {
                this.setState({ componentPath });
            }
        },

        render() {
            const { registry, ...passthru } = this.props;
            const { componentPath } = this.state;

            return (
                <Attacher
                    ref={r => { this._ref = r }}
                    componentPath={componentPath}
                    behavior={{
                        onVisibilityChange: this.onVisibilityChange
                    }}
                    {...passthru}
                />
            );
        },

        onVisibilityChange(inst, data) {
            const { onVisibilityChange, legacyOnVisibilityChange } = this.props;

            if (onVisibilityChange) {
                onVisibilityChange(data)
            } else if (legacyOnVisibilityChange) {
                legacyOnVisibilityChange(data);
            }
        },

        onVisibilityClear() {
            const { componentPath } = this.state;
            const component = this.asdf;

            if (component) {
                if (_.isFunction(component.onClear)) {
                    component.onClear();
                } else if (!(componentPath in apiWarnings)) {
                    console.warn(`${componentPath} missing onClear method, see the documentation for org.visallo.visibility extension point`);
                    apiWarnings[componentPath] = true;
                }
            }
        }
    });

    return redux.connect(
        (state, props) => {
            return {
                authorizations: userSelectors.getAuthorizations(state)
            };
        }
    )(RegistryInjectorHOC(VisibilityEditor, [
        'org.visallo.visibility'
    ]));
});
