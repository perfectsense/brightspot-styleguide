const Chance = require('chance');
const expect = require("chai").expect;
const assert = require("chai").assert;
const DataGenerator = require("../lib/data-generator");

describe("Date Generator", function() {

    var seed = 1
    var chance = new Chance(1)
    var chanceDate = chance.date()

    var defaultDate = new DataGenerator({
        "seed": seed,
        "test": "{{date()}}"
    }).date()

    var unformattedDate = new DataGenerator({
        "seed": seed,
        "test": "{{date('unformatted')}}"
    }).date('unformatted')

    var shortDate = new DataGenerator({
        "seed": seed,
        "test": "{{date('short')}}"
    }).date('short')

    var isoDate = new DataGenerator({
        "seed": seed,
        "test": "{{date('iso')}}"
    }).date('iso')

    it("should never have a trailing space", function() {
        expect(/\s$/.test(defaultDate)).to.be.false
        expect(/\s$/.test(unformattedDate)).to.be.false
        expect(/\s$/.test(shortDate)).to.be.false
        expect(/\s$/.test(isoDate)).to.be.false
    })

    it("should be a valid Date type", function() {
        assert.typeOf(new Date(defaultDate), 'date', 'can be converted back to a Date object')
        assert.typeOf(new Date(unformattedDate), 'date', 'can be converted back to a Date object')
        assert.typeOf(new Date(shortDate), 'date', 'can be converted back to a Date object')
        assert.typeOf(new Date(isoDate), 'date', 'can be converted back to a Date object')
    })

    describe("called as `date()`", function() {
        it("should be the Date as a string", function() {
            assert.typeOf(defaultDate, 'string', 'it is a Date String')
        })
    })

    describe("called as `date('unformatted')`", function() {
        it("should be a Date object", function(){
            assert.typeOf(unformattedDate, 'date', 'it is a Date object')
        })

        it("should equal the same time as the Chance Date", function(){
            expect(unformattedDate.getTime()).to.equal(chanceDate.getTime())
        })
    })

    describe("called as `date('short')`", function() {
        it("should be the Date as a string", function() {
            assert.typeOf(shortDate, 'string', 'it is a Date String')
        })
    })

    describe("called as `date('iso')`", function() {
        it("should be the Date as a string", function() {
            assert.typeOf(isoDate, 'string', 'it is a Date String')
        })
    })
});
