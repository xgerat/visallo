define([
    'prop-types',
    'create-react-class',
    'react-sortable-hoc',
    './MapLayersList',
    './MapLayerDetail',
    '../util/layerHelpers'
], function(
    PropTypes,
    createReactClass,
    { arrayMove },
    MapLayersList,
    MapLayerDetail,
    layerHelpers) {

    const UPDATE_DEBOUNCE = 300;

    const MapLayers = createReactClass({

        propTypes: {
            product: PropTypes.shape({
                extendedData: PropTypes.shape({
                    vertices: PropTypes.object,
                    edges: PropTypes.object }
                ).isRequired
            }).isRequired,
            map: PropTypes.object.isRequired,
            baseLayer: PropTypes.object,
            layersConfig: PropTypes.object,
            layerOrder: PropTypes.array.isRequired,
            layerIds: PropTypes.array.isRequired,
            layers: PropTypes.array.isRequired,
            editable: PropTypes.bool,
            setLayerOrder: PropTypes.func.isRequired,
            updateLayerConfig: PropTypes.func.isRequired
        },

        getInitialState() {
            return { futureIndex: null, selected: null }
        },

        componentDidMount() {
            this.onUpdateLayerConfig = _.debounce(this._onUpdateLayerConfig, UPDATE_DEBOUNCE)
        },

        componentWillReceiveProps(nextProps) {
            if (nextProps.layerOrder !== this.props.layerOrder && this.state.futureIndex) {
                this.setState({ futureIndex: null });
            }
        },

        render() {
            const { futureIndex, selected } = this.state;
            const { baseLayer, layers, layersConfig, editable, ol, map } = this.props;
            let layerList = futureIndex ? arrayMove(layers, futureIndex[0], futureIndex[1]) : layers;
            layerList = layerList.map(layer => ({
                config: layersConfig[layer.get('id')],
                layer
            }));

            let selectedLayer;
            if (selected) {
                selectedLayer = selected === 'base' ? baseLayer : layers.find(layer => layer.get('id') === selected);
            }

            return (
                <div className="map-layers">
                    {!selectedLayer ?
                        <MapLayersList
                            baseLayer={{ config: layersConfig['base'], layer: baseLayer }}
                            layers={layerList}
                            editable={editable}
                            onConfigureLayer={this.onConfigureLayer}
                            onToggleLayer={this.onToggleLayer}
                            onSelectLayer={this.onSelectLayer}
                            onOrderLayer={this.onOrderLayer}
                        />
                    :
                        <MapLayerDetail
                            layer={selectedLayer}
                            config={layersConfig[selected]}
                            onBack={this.onBack}
                            onUpdateLayerConfig={this.onUpdateLayerConfig}
                        />}
                </div>
            );
        },

        onSelectLayer(layerId) {
            this.setState({ selected: layerId });
        },

        onBack() {
            this.setState({ selected: false });
        },

        onOrderLayer(oldSubsetIndex, newSubsetIndex) {
            const { product, layerIds, layerOrder, setLayerOrder } = this.props;
            const orderedSubset = arrayMove(layerIds, oldSubsetIndex, newSubsetIndex);

            const oldIndex = layerOrder.indexOf(orderedSubset[newSubsetIndex]);
            let newIndex;
            if (newSubsetIndex === orderedSubset.length - 1) {
                const afterId = orderedSubset[newSubsetIndex - 1];
                newIndex = layerOrder.indexOf(afterId);
            } else {
                const beforeId = orderedSubset[newSubsetIndex + 1];
                const displacementOffset = oldSubsetIndex > newSubsetIndex ? 0 : 1;
                newIndex = Math.max((layerOrder.indexOf(beforeId) - displacementOffset), 0);
            }

            //optimistically update item order in local component state so it doesn't jump
            this.setState({ futureIndex: [ oldSubsetIndex, newSubsetIndex ]});

            setLayerOrder(arrayMove(layerOrder, oldIndex, newIndex));
        },

        onToggleLayer(layer) {
            const { product, layersConfig, updateLayerConfig } = this.props;

            const layerId = layer.get('id');
            const config = { ...(layersConfig[layerId] || {}), visible: !layer.getVisible() };

            layerHelpers.setLayerConfig(config, layer);
            updateLayerConfig(config, layerId);
        },

        _onUpdateLayerConfig(config, layer) {
            layerHelpers.setLayerConfig(config, layer);
            this.props.updateLayerConfig(config, layer.get('id'));
        }

    });

    return MapLayers;
});
