define(['cytoscape'], function(cytoscape) {

    function getImpl(type) {
        switch (type) {
            case 'circle':
                return import(
                    /* webpackChunkName: "layout-circle" */
                    './circle'
                );
            case 'grid':
                return import(
                    /* webpackChunkName: "layout-grid" */
                    './grid'
                );
            case 'hierarchical':
                return import(
                    /* webpackChunkName: "layout-hierarchical" */
                    './hierarchical'
                );
            case 'force':
                return import(
                    /* webpackChunkName: "layout-force" */
                    './force'
                );
        }
    }

    function GenericLayout(type, options = {}) {
        this.type = type;
        this.options = options;
        this.layoutImpl = getImpl(type);
    }

    GenericLayout.prototype.run = function() {
        this.layoutImpl.then(layout => {
            return layout.call(this, this.options, cytoscape);
        }).catch(e => {
            console.error(e);
        })
        return this;
    };

    GenericLayout.prototype.calculations = function(options = {}, extraCallback) {
        const center = { x: 0, y: 0 };
        const size = { min: { w: 0, h: 0 }, max: { w: 0, h: 0 } };
        const nodes = this.options.eles.filter('node');
        nodes.forEach((el, i, list) => {
            const { w, h } = el.layoutDimensions({ ...this.options, ...options });
            const { x, y } = el.position();
            center.x += x;
            center.y += y;
            size.max.w = Math.max(size.max.w, w);
            size.max.h = Math.max(size.max.h, h);
            size.min.w = Math.max(size.min.w, w);
            size.min.h = Math.max(size.min.h, h);
            if (extraCallback) {
                extraCallback(el, { x, y, w, h }, i, list)
            }
        })

        const len = nodes.length;
        if (len) {
            return {
                center: { x: center.x / len, y: center.y / len },
                size
            }
        }
    };


    function registerLayouts(cytoscape) {
        [
            'circle',
            'grid',
            'hierarchical',
            'force'
        ].forEach(layout => {
            const Layout = function(options) {
                GenericLayout.call(this, layout, options)
            };
            Layout.prototype = GenericLayout.prototype;
            cytoscape('layout', 'custom-' + layout, Layout);
        })
    }

    return registerLayouts;
})
