export interface ToRenderOutputArg {
    result: string;
    renderedName: string;
    dependencies?: string[];
}
/**
 * Common representation for the rendered output.
 */
export declare class RenderOutput {
    readonly result: string;
    readonly renderedName: string;
    readonly dependencies: string[];
    constructor(result: string, renderedName: string, dependencies?: string[]);
    static toRenderOutput(args: ToRenderOutputArg): RenderOutput;
}
//# sourceMappingURL=RenderOutput.d.ts.map