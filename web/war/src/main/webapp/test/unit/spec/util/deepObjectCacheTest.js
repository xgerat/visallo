define([
    'util/deepObjectCache'
], function(DeepObjectCache) {

    describe('DeepObjectCache', function() {
        before(function() {
            this.callFn = function(...args) {
                return this.cache.getOrUpdate(this[`fn${args.length}Arg`], ...args)
            }
        })

        beforeEach(function() {
            this.cache = new DeepObjectCache()
            for (let i = 0; i < 4; i++) {
                this[`fn${i}Arg`] = sinon.spy((...args) => {
                    return args.reduce((result, a) => {
                        if (result) {
                            return result + '|' + a.result
                        }
                        return a.result
                    }, '')
                })
            }
        })

        it('should error when no new', function() {
            expect(DeepObjectCache).to.throw('new')
        })

        it('should clear', function() {
            const times = 3;
            const arg = { result: 'x' }
            for (let i = 0; i < times; i++) {
                this.callFn(arg)
            }
            this.fn1Arg.callCount.should.equal(1)

            for (let i = 0; i < times; i++) {
                this.cache.clear();
                this.callFn(arg)
            }
            this.fn1Arg.callCount.should.equal(times + 1)
        })

        it('should evaluate when no cache', function() {
            let result = this.callFn({ result: 'x'})
            result.should.equal('x')
            this.fn1Arg.callCount.should.equal(1)
        })

        it('should only evaluate once when no changes', function() {
            const p1 = { result: 'x'}
            let result = this.callFn(p1)
            let result2 = this.callFn(p1)
            result.should.equal('x')
            result.should.equal(result2)
            this.fn1Arg.callCount.should.equal(1)
        })
        
        it('should re-evaluate when args change', function() {
            const p1 = { result: 'x'}
            const p2 = { result: 'y'}
            let result = this.callFn(p1)
            let result2 = this.callFn(p2)
            result.should.equal('x')
            result2.should.equal('y')
            this.fn1Arg.callCount.should.equal(2)
        })

        it('should re-evaluate when args are equal but not identity', function() {
            const p1 = { result: 'x'}
            const p2 = { result: 'x'}
            let result = this.callFn(p1)
            let result2 = this.callFn(p2)
            result.should.equal('x')
            result2.should.equal('x')
            this.fn1Arg.callCount.should.equal(2)
        })

        it('should re-evaluate when args arity changes (less)', function() {
            const fn = sinon.spy(function(...args) {
                return args.map(a => a.result).join(',')
            })
            const p1 = { result: 'x'}
            const p2 = { result: 'y'}
            let result = this.cache.getOrUpdate(fn, p1, p2)
            let result2 = this.cache.getOrUpdate(fn, p1)
            let result3 = this.cache.getOrUpdate(fn, p1, p2)
            let result4 = this.cache.getOrUpdate(fn, p2, p1)
            result.should.equal('x,y')
            result2.should.equal('x')
            result3.should.equal('x,y')
            result4.should.equal('y,x')
            fn.callCount.should.equal(4)
        })

        it('should re-evaluate when args arity changes (more)', function() {
            const fn = sinon.spy(function(...args) {
                return args.map(a => a.result).join(',')
            })
            const p1 = { result: 'x'}
            const p2 = { result: 'y'}
            let result = this.cache.getOrUpdate(fn, p1)
            let result2 = this.cache.getOrUpdate(fn, p1, p2)
            result.should.equal('x')
            result2.should.equal('x,y')
            fn.callCount.should.equal(2)
        })

    })

})
