"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.GeneratorError = void 0;
class GeneratorError extends Error {
    constructor(err) {
        super();
        this.name = 'Generator Error';
        this.message = err.message;
    }
}
exports.GeneratorError = GeneratorError;
