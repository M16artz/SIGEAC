export type ModelStatus = 'DRAFT' | 'PUBLISHED' | 'SUPERSEDED';
export interface EvaluationModel {id:string;name:string;description:string;createdBy:string;createdAt:string}
export interface ModelNode {id?:string;code:string;name:string;description:string;position:number;children:readonly ModelNode[]}
export interface ScaleOption {id?:string;code:string;label:string;position:number;declaredValue?:number}
export interface RatingScale {id?:string;code:string;name:string;options:readonly ScaleOption[]}
export interface ProgressFormula {code:string;schema:string;results:readonly string[];inputs:readonly string[];zeroPolicy:string;notApplicablePolicy:string;examples:readonly {input:Record<string,unknown>;expected:Record<string,unknown>}[];parameters:Record<string,unknown>}
export interface VersionDraft {source:string;criteria:readonly ModelNode[];scale:RatingScale|null;formula:ProgressFormula|null}
export interface ModelVersion extends VersionDraft {id:string;modelId:string;versionNumber:number;status:ModelStatus;authorId:string;approverId?:string;predecessorId?:string;revision:number;createdAt:string;publishedAt?:string}
export interface ModelComparison {fromVersionId:string;toVersionId:string;changes:readonly {key:string;kind:'ADDED'|'REMOVED'|'MODIFIED'|'UNCHANGED'}[]}
