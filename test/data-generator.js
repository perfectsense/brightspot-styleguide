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

    it("should return a valid Date type", function() {
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

describe("Number Generator", function() {

    var seed = 1
    var numberArg = 10
    var chance = new Chance(1)
    var result

    describe("called as `number()`", function() {
        it("should throw an Error when called with a value that's not a number", function(){
            expect(new DataGenerator({}).number()).to.be.an('error')
        })
    })

    describe("called as `number("+ numberArg +")`", function() {
        result = new DataGenerator({
            "seed": seed,
            "test": "{{number()}}"
        }).number(numberArg)

        it("should return a number between 0 and the argument inclusive when called with a single number argument", function() {
            assert.isAtLeast(result, 0, result + " is greater or equal to 0");
            assert.isBelow(result, result + 1, result + " is strictly less than " + (result + 1));
        })

        it("should return a valid Number type", function() {
            assert.isNumber(result, 'number', "can be converted back to a Number object")
        })
    })

    var numberArgRange = [5,10]
    describe("called as `number(["+ numberArgRange.toString() +"])`", function() {
        result = new DataGenerator({
            "seed": seed,
            "test": "{{number()}}"
        }).number(numberArgRange)

        it("should return a number within the range inclusive when called with an Array argument", function() {
            assert.isAtLeast(result, numberArgRange[0], result + " is greater or equal to 0");
            assert.isBelow(result, numberArgRange[1] + 1, result + " is strictly less than " + (numberArgRange[1] + 1));
        })

        it("should return a valid Number type", function() {
            assert.isNumber(result, 'number', "can be converted back to a Number object")
        })
    })
});
