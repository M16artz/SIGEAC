import Command from '../../../base';
export default class ContextCurrent extends Command {
    static description: string;
    static flags: {
        help: import("@oclif/core/lib/interfaces").BooleanFlag<void>;
    };
    run(): Promise<void>;
}
