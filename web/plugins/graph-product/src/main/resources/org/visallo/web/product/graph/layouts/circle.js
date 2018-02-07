define(['./circleSimple', './circleConcentric'], function(simple, concentric) {

    return function circleLayout(options = {}) {
        const { eles } = options;
        const nodes = eles.filter('node');

        if (nodes.maxDegree() === 0) {
            return simple.call(this, options);
        } else {
            return concentric.call(this, options);
        }
    }
})
