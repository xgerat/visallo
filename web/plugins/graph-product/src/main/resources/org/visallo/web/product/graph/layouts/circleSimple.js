define([], function() {

    const NodesPerRing = 25;

    return function circleSimpleLayout(options = {}) {
        const { eles } = options;
        const { size, center } = this.calculations({ nodeDimensionsIncludeLabels: true });
        const nodes = eles.filter('node');

        let angle = 0;

        const theta = Math.PI * 2 / nodes.length;
        const nodeDiameter = Math.max(size.max.w, size.max.h);
        const diameterCos = Math.cos(theta) - Math.cos(0);
        const diameterSin = Math.sin(theta) - Math.sin(0);
        const rMin = Math.sqrt(
            nodeDiameter * nodeDiameter /
            ( diameterCos * diameterCos + diameterSin * diameterSin)
        );
        const radius = Math.max(10, rMin);
        const getPosition = node => {
            const position = {
                x: radius * Math.sin(angle) + center.x,
                y: radius * Math.cos(angle) + center.y
            };
            angle += theta;
            return position;
        };

        nodes.layoutPositions(this, options, getPosition);
    }
})
