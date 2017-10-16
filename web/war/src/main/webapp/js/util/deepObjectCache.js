define([], function() {

    DeepObjectCache.prototype.getOrUpdate = getOrUpdate;
    DeepObjectCache.prototype.clear = clear;

    return DeepObjectCache;

    /*
     * Cache that uses weak maps to cache results of functions given object
     * arguments.
     *
     * Useful for caching calls to registry extension functions given object
     * arguments. All arguments must be objects (non-primitive) as they are
     * used as WeakMap keys.
     *
     * Input objects must be immutable otherwise changes won't be
     * detected/reevaluated.
     *
     *  var c = new DeepObjectCache();
     *  c.getOrUpdate(expensiveFn, input1, input2);
     *  // Calls expensiveFn(input1, input2) once until inputs or arity changes
     */
    function DeepObjectCache() {
        if (this === window) throw new Error('Must instantiate cache with new')
    }

    function clear() {
        if (this.weakMap) {
            this.weakMap = null;
        }
    }

    function getOrUpdate(fn, ...args) {
        if (!_.isFunction(fn)) throw new Error('fn must be a function');
        if (!args.length) throw new Error('Must have at least one argument');

        if (!this.weakMap) this.weakMap = new WeakMap();

        return _getOrUpdate(this.weakMap, [fn, ...args], reevaluate)

        function reevaluate() {
            return fn.apply(null, args);
        }
    }

    function _getOrUpdate(cache, keyObjects, reevaluate) {
        if (keyObjects.length === 0) {
            return cache
        }

        const nextKey = keyObjects.shift();
        let nextObject = cache.get(nextKey);
        if (nextObject) {
            // Check for arity changes and clear
            if (nextObject instanceof WeakMap && keyObjects.length === 0) {
                nextObject = reevaluate();
            } else if (!(nextObject instanceof WeakMap) && keyObjects.length) {
                cache.delete(nextKey)
                nextObject = new WeakMap();
            }
        } else {
            nextObject = keyObjects.length ? new WeakMap() : reevaluate();
        }

        cache.set(nextKey, nextObject);

        return _getOrUpdate(nextObject, keyObjects, reevaluate);
    }
})
