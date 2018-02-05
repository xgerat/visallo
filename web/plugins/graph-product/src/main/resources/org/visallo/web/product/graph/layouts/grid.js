define([], function() {

    return function gridLayout(options = {}) {
        let { spaceX = 0, spaceY = 0, padding = 25, ...otherOptions} = options;

        spaceX *= devicePixelRatio;
        spaceY *= devicePixelRatio;
        padding *= devicePixelRatio;

        const { eles: nodes } = otherOptions;
        const bb = nodes.boundingBox();
        const len = nodes.length;

        const { center, size } = this.calculations();
        spaceX = Math.max(size.max.w, spaceX);
        spaceY = Math.max(size.max.h, spaceY);
        spaceX += padding;
        spaceY += padding;

        const columns = Math.round(Math.sqrt(len * (spaceY / spaceX)));
        const rows = Math.ceil(len / columns);
        const linebreak = columns * spaceX;

        const startX = center.x - (linebreak / 2);
        const startY = center.y - ((rows * spaceY) / 2);
        let x = startX, y = startY;
        const getPosition = node => {
            if ((x - startX) > linebreak) {
                x = startX;
                y += spaceY;
            }

            var position = { x: x, y: y };
            x += spaceX;
            return position;
        };

        nodes.layoutPositions(this, otherOptions, getPosition);
    }
})
