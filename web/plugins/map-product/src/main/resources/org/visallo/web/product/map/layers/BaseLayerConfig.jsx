define([
    'prop-types',
    'create-react-class',
    'react-redux'
], function(
    PropTypes,
    createReactClass,
    redux) {

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
                        value={config.provider || defaultProvider}
                        onChange={this.onSelectProvider}
                    >
                        {mapProviders.map(({ name, label }) => <option key={name} value={name}>{ label }</option>)}
                    </select>
                </div>
            )
        },

        onSelectProvider(event) {
            const { layer, config = {}, onUpdateLayerConfig } = this.props;
            onUpdateLayerConfig({ ...config, provider: event.target.value }, layer);
        }
    });

    return redux.connect(
        (state, props) => {
            const properties = state.configuration.properties;
            const defaultProvider = properties['map.provider'] || 'osm';
            const mapProviders = _.chain(properties)
                .pick((value, property) => property.startsWith('map.provider.'))
                .reduce((providers, value, key) => {
                    const [ str, provider ] = key.match(/^map\.provider\.(.+)\..*$/);

                    if (provider && !providers[provider]) {
                        providers[provider] = { name: provider, label: provider };
                    }
                    if (key.endsWith('label')) {
                        providers[provider].label = value;
                    }

                    return providers;
                }, { [defaultProvider]: { name: defaultProvider, label: defaultProvider }})
                .values()
                .value();

            return {
                defaultProvider,
                mapProviders
            }

        }
    )(BaseLayerConfig);
});
