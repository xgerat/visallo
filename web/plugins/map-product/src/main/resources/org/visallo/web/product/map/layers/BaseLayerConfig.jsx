define([
    'prop-types',
    'create-react-class',
    'classnames',
], function(
    PropTypes,
    createReactClass) {

    const BaseLayerConfig = createReactClass({
        propTypes: {
            // layer: PropTypes.object.isRequired,
            // config: PropTypes.object,
            // onUpdateLayerConfig: PropTypes.func
        },

        render() {
            return (
                <div className="base-layer-provider">
                    <button onClick={this.onToggleProvider}>Toggle Provider</button>
                </div>
            )
        },

        onToggleProvider() {
            const { layer, config = {}, onUpdateLayerConfig } = this.props;
            if (layer.get('id') === 'base') {
                const { provider: prevProvider, ...rest } = config;

                let provider = !prevProvider || prevProvider === 'osm' ? 'BingMaps' : 'osm';

                onUpdateLayerConfig({ ...config, provider }, layer);
            }
        }
    });

    return BaseLayerConfig;
});
