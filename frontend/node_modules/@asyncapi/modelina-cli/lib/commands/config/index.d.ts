import Command from '../../base';
export default class Config extends Command {
    static description: string;
    run(): Promise<void>;
}
