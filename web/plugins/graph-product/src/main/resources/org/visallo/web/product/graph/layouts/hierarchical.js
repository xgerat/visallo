define([
    'cytoscape-dagre',
    'dagre'
], function(
    cytoscapeDagre,
    dagre) {

    const register = _.once(cytoscape => {
        cytoscapeDagre(cytoscape, dagre);
    });

    return function hierarchicalLayout(options, cytoscape) {
        register(cytoscape);

        const { cy } = options;
        const DagreLayout = cy.extension('layout', 'dagre');
        const layout = new DagreLayout({ spacingFactor: 1.3, ...options });

        layout.run();
    }
})
