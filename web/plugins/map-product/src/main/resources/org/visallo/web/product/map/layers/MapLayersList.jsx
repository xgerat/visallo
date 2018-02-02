define([
    'prop-types',
    'create-react-class',
    'react-virtualized',
    './SortableList',
    './MapLayerItem'
], function(
    PropTypes,
    createReactClass,
    { AutoSizer },
    SortableList,
    MapLayerItem) {

    const ROW_HEIGHT = 40;
    const SORT_DISTANCE_THRESHOLD = 10;

    const MapLayersList = createReactClass({
        propTypes: {
            baseLayer: PropTypes.object,
            layers: PropTypes.array.isRequired,
            editable: PropTypes.bool,
            onOrderLayer: PropTypes.func.isRequired
        },

        componentWillMount() {
          this.getRowHeight = _.memoize(getRowHeight, (selected, layer) => `${selected}:${layer.get('id')}`);
        },

        componentDidUpdate(prevProps, prevState) {
            if (prevProps.selected !== this.props.selected) {
                this.SortableList.refs.wrappedInstance.List.recomputeRowHeights();
                this.NonSortableList.refs.wrappedInstance.List.recomputeRowHeights();
            }
        },

        render() {
            const {baseLayer, layers, editable, onOrderLayer, selected, ...itemProps} = this.props;
            const nonSortableLayers = [baseLayer];
            const sortableLayers = [];

            layers.forEach(layer => {
                if (layer.sortable !== false) {
                    sortableLayers.push(layer);
                } else {
                    nonSortableLayers.push(layer);
                }
            });

            const maxNonSortableHeight = ROW_HEIGHT * 3;
            const nonSortableHeight = selected
                ? nonSortableLayers.reduce((height, l) =>
                    Math.max(this.getRowHeight(selected, l.layer), height)
                , ROW_HEIGHT)
                : ROW_HEIGHT * nonSortableLayers.length;


            return (
                <div className="layer-list">
                    {(baseLayer || layers.values) ?
                        <div className="layers">
                            <div className="flex-fix">
                                <AutoSizer>
                                    {({width, height}) => ([
                                        <SortableList
                                            ref={(instance) => {
                                                this.SortableList = instance;
                                            }}
                                            key={'sortable-items'}
                                            items={layers}
                                            shouldCancelStart={() => !editable}
                                            onSortStart={() => {
                                                this.SortableList.container.classList.add('sorting')
                                            }}
                                            onSortEnd={({oldIndex, newIndex}) => {
                                                this.SortableList.container.classList.remove('sorting');

                                                if (oldIndex !== newIndex) {
                                                    onOrderLayer(oldIndex, newIndex);
                                                }
                                            }}
                                            rowRenderer={mapLayerItemRenderer({editable, selected, ...itemProps})}
                                            rowHeight={({index}) => {
                                                return this.getRowHeight(selected, sortableLayers[index].layer)
                                            }}
                                            estimatedRowSize={ROW_HEIGHT}
                                            lockAxis={'y'}
                                            lockToContainerEdges={true}
                                            helperClass={'sorting'}
                                            distance={SORT_DISTANCE_THRESHOLD}
                                            width={width}
                                            height={(height - (Math.min(nonSortableHeight, maxNonSortableHeight) + 1))}
                                        />,
                                        <SortableList
                                            ref={(instance) => {
                                                this.NonSortableList = instance;
                                            }}
                                            key={'non-sortable-items'}
                                            className="unsortable"
                                            items={[baseLayer]}
                                            shouldCancelStart={() => true}
                                            rowRenderer={mapLayerItemRenderer({editable, selected, ...itemProps})}
                                            rowHeight={({index}) => {
                                                return this.getRowHeight(selected, nonSortableLayers[index].layer)
                                            }}
                                            estimatedRowSize={ROW_HEIGHT}
                                            width={width}
                                            height={Math.min(nonSortableHeight, maxNonSortableHeight)}
                                        />
                                    ])}
                                </AutoSizer>
                            </div>
                        </div>
                        :
                        <div className="empty">
                            <p>{i18n('org.visallo.web.product.map.MapWorkProduct.layers.empty')}</p>
                        </div>
                    }
                </div>
            );
        }
    });

    const mapLayerItemRenderer = (itemProps) => (listProps) => {
        const { editable, selected, ...rest } = itemProps;
        const { index, style, key, value: { config, layer }} = listProps;

        return (
            <MapLayerItem
                key={key}
                index={index}
                layer={layer}
                config={config}
                extension={'layer'}
                style={style}
                toggleable={editable}
                selected={layer.get('id') === selected}
                {...rest}
            />
        )
    };

    return MapLayersList;

    function getRowHeight(selected, layer) {
        let rowHeight = ROW_HEIGHT;
        const { id, config } = layer.getProperties();

        if (config && selected === id) {
            rowHeight += config.height || ROW_HEIGHT;
        }

        return rowHeight;
    }
});
