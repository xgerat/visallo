define([
    'flight/lib/component',
    './util'
], function(defineComponent, util) {
    'use strict';

    //legacy flight wrapper, prefer components/visibility/VisibilityViewer
    return defineComponent(VisibilityViewerContainer);

    function VisibilityViewerContainer() {

        this.before('teardown', function() {
            if (this.attacher && _.isFunction(this.attacher.teardown)) {
                this.attacher.teardown();
            }
        });

        this.after('initialize', function() {
            this.attacher = util.attachComponent('viewer', this.node, this.attr);
        });
    }
});


