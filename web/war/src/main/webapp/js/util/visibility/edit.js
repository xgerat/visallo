define([
    'flight/lib/component',
    './util'
], function(defineComponent, util) {
    'use strict';

    const propertyKeys = ['value', 'placeholder', 'readonly'];

    // legacy flight wrapper, prefer components/visibility/VisibilityEditor
    return defineComponent(VisibilityEditorContainer);

    function VisibilityEditorContainer() {

        this.before('teardown', function() {
            if (this.attacher && _.isFunction(this.attacher.teardown)) {
                this.attacher.teardown();
            }
        });

        this.after('initialize', function() {
            const props = propertyKeys.reduce((acc, key) => {
                if (this.attr[key] !== undefined) {
                    acc[key] = this.attr[key];
                }
                return acc;
            }, {})

            this.attacher = util.attachComponent('editor', this.node, {
                ...props,
                legacyOnVisibilityChange: legacyOnVisibilityChange.bind(this)
            });

            /**
             * The user has adjusted the visibility so notify if no onVisibilityChange handler was provided
             *
             * @deprecated use the onVisibilityChange callback instead
             * @event org.visallo.visibility#visibilitychange
             * @param {object} data
             * @param {string} data.value The new visibility value
             * @param {boolean} data.valid Whether the value is valid
             */
            function legacyOnVisibilityChange(data) {
                this.trigger('visibilitychange', data);
            }
        });
    }
});



