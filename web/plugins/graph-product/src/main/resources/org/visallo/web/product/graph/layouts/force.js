define(['weaverjs'], function(weaver) {
    return function forceLayout(options) {
        const { cy, eles } = options;
        const CoseLayout = cy.extension('layout', 'cose');
        const layout = new CoseLayout({
            weaver,
            edgeElasticity: 10,
            nodeDimensionsIncludeLabels: true,
            ...options
        });

        // Force directed has issues if we don't pass the decoration
        // parents (because we don't want those to layout, so just hack
        // them to not show parents.
        eles.forEach(({_private: el }) => {
            if (el.parent || el.data.parent) {
                el._restoreParent = el.parent;
                el._restoreDataParent = el.data.parent;
                el.parent = null;
                el.data.parent = null;
            }
        })

        layout.run();

        eles.forEach(({ _private: el }) => {
            if (el._restoreParent || el._restoreDataParent) {
                el.parent = el._restoreParent;
                el.data.parent = el._restoreDataParent;
                delete el._restoreParent;
                delete el._restoreDataParent;
            }
        })
    }
})
