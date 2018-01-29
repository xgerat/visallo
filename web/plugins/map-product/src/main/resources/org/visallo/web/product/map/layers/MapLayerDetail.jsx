define([
    'create-react-class',
    'prop-types',
    'classnames',
    'components/Attacher',
    'util/vertex/formatters'
], function(
    createReactClass,
    PropTypes,
    classNames,
    Attacher,
    F) {

    const MapLayerDetail = createReactClass({
        propTypes: {
            layer: PropTypes.object.isRequired,
            config: PropTypes.object
        },

        render() {
            const {layer, config, onBack, onUpdateLayerConfig } = this.props;
            const layerStatus = layer.get('status');
            const statusMessage = (_.isObject(layerStatus) && layerStatus.message) || null;
            const hasError = _.isObject(layerStatus) && layerStatus.type === 'error';

            return (
                <div>
                    <div className={classNames('layer-item', {'error': hasError})} style={{ zIndex: 50}}>
                        <div className="layer-title">
                            <div className="title">{titleRenderer(layer)}</div>
                            <span className="subtitle" title={statusMessage}>{statusMessage}</span>
                        </div>
                        <span onClick={onBack}>back</span>
                    </div>
                    <button onClick={this.onToggleProvider}>Toggle Provider</button>
                </div>
            )
        },

        onToggleProvider() {
            const { layer, config, onUpdateLayerConfig } = this.props;
            if (layer.get('id') === 'base') {
                const { provider: prevProvider, ...rest } = config;

                let provider = prevProvider && prevProvider === 'osm' ? 'BingMaps' : 'osm';

                onUpdateLayerConfig({ ...config, provider }, layer);
            }
        }
    });

    const titleRenderer = (layer) => {
        const { label, element } = layer.getProperties();

        if (label) {
            return label;
        } else if (element) {
            return F.vertex.title(element);
        } else {
            return i18n('org.visallo.web.product.map.MapWorkProduct.layer.no.title');
        }
    };

    return MapLayerDetail;
});
