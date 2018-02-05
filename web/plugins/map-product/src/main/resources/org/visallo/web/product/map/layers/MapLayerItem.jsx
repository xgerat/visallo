define([
    'prop-types',
    'classnames',
    'react-sortable-hoc',
    'components/Attacher',
    'util/vertex/formatters'
], function(
    PropTypes,
    classNames,
    { SortableElement },
    Attacher,
    F) {

    const MapLayerItem = ({ layer, config, style, selected, toggleable, onToggleLayer, onSelectLayer, onUpdateLayerConfig }) => {
        const layerStatus = layer.get('status');
        const statusMessage = (_.isObject(layerStatus) && layerStatus.message) || null;
        const hasError = _.isObject(layerStatus) && layerStatus.type === 'error';
        const visible = config && config.visible !== undefined ? config.visible : layer.getVisible();
        const configComponent = layer.get('config');

        return (
            <div className={'layer-item'} style={{ ...style, zIndex: 50 }}>
                <div className={'list-item'} style={{ 'height': '40px'}}>
                    <div
                        className={classNames('layer-header', { 'error': hasError })}
                        onClick={() => { onSelectLayer(layer.get('id'))}}
                    >
                        <input
                            type="checkbox"
                            checked={visible}
                            disabled={!toggleable || hasError}
                            onChange={(e) => { onToggleLayer(layer)}}
                            onClick={(e) => { e.stopPropagation() }}
                        />
                        <div className="layer-title">
                            <div className="title">{ titleRenderer(layer) }</div>
                            <span className="subtitle" title={statusMessage}>{ statusMessage }</span>
                        </div>
                        <div
                            className="layer-icon drag-handle"
                            title={i18n('org.visallo.web.product.map.MapWorkProduct.layers.sort.help')}
                        ></div>
                    </div>
                </div>
                {selected && configComponent ?
                    <div className={'layer-dropdown'} style={{'height': layer.get('config').height}}>
                        <Attacher
                            componentPath={layer.get('config').componentPath}
                            layer={layer}
                            config={config}
                            onUpdateLayerConfig={onUpdateLayerConfig}
                        />
                    </div>
                : null}

            </div>
        )
    };

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

    MapLayerItem.propTypes = {
        layer: PropTypes.object.isRequired,
        config: PropTypes.object,
        style: PropTypes.object,
        toggleable: PropTypes.bool,
        onToggleLayer: PropTypes.func.isRequired
    };

    return SortableElement(MapLayerItem);
});
