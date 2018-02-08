define([
    'prop-types',
    'create-react-class',
    'react-redux',
    '../worker/selectors'
], function(
    PropTypes,
    createReactClass,
    redux,
    mapSelectors) {

    const BaseLayerConfig = createReactClass({
        propTypes: {
            layer: PropTypes.object.isRequired,
            config: PropTypes.object,
            onUpdateLayerConfig: PropTypes.func.isRequired
        },

        render() {
            const { mapProviders, defaultProvider, config = {} } = this.props;

            return (
                <div className={'base-layer-dropdown'}>
                    <span className={'map-providers-label'}>{ i18n('org.visallo.web.product.map.MapWorkProduct.layers.base.provider') }</span>
                    <select
                        className={'map-providers'}
                        value={config.providerName || defaultProvider}
                        onChange={this.onSelectProvider}
                    >
                        {mapProviders.map(({ providerName, label }) => <option key={providerName} value={providerName}>{ label }</option>)}
                    </select>
                </div>
            )
        },

        onSelectProvider(event) {
            const { layer, config = {}, mapProviders, onUpdateLayerConfig } = this.props;
            const { providerName, providerSource: nextProviderSource } = mapProviders.find(p => p.providerName === event.target.value);
            const { providerSource, ...prevConfig } = config;
            const nextConfig = { ...prevConfig, providerName };

            if (nextProviderSource) {
                nextConfig.providerSource = nextProviderSource;
            }

            onUpdateLayerConfig(nextConfig, layer);
        }
    });

    return redux.connect(
        (state, props) => ({
            defaultProvider: mapSelectors.getDefaultProvider(state),
            mapProviders: mapSelectors.getProviders(state)
        })
    )(BaseLayerConfig);
});
