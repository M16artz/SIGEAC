"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const tslib_1 = require("tslib");
const base_1 = tslib_1.__importDefault(require("../../base"));
const core_1 = require("@oclif/core");
class Config extends base_1.default {
    run() {
        return tslib_1.__awaiter(this, void 0, void 0, function* () {
            const Help = yield (0, core_1.loadHelpClass)(this.config);
            const help = new Help(this.config);
            help.showHelp(['config', '--help']);
        });
    }
}
exports.default = Config;
Config.description = 'CLI config settings';
