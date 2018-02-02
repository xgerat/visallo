define([
    'prop-types',
    'create-react-class'
], function(
    PropTypes,
    createReactClass) {

    const BaseLayerConfig = createReactClass({
        propTypes: {
            layer: PropTypes.object.isRequired,
            config: PropTypes.object,
            onUpdateLayerConfig: PropTypes.func.isRequired
        },

        render() {
            return (
                <div className={'base-layer-provider'}>
                    <button onClick={this.onToggleProvider}>Toggle Provider</button>
                </div>
            )
        },

        onToggleProvider() {
            const { layer, config = {}, onUpdateLayerConfig } = this.props;
            let provider = !config.provider || config.provider === 'osm' ? 'BingMaps' : 'osm';

            onUpdateLayerConfig({ ...config, provider }, layer);
        }
    });

    return BaseLayerConfig;
});
