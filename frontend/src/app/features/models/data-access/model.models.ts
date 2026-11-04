export type ModelStatus = 'DRAFT' | 'PUBLISHED' | 'SUPERSEDED';
export type NodeType = 'CRITERION' | 'SUBCRITERION' | 'INDICATOR';
export type VerificationStatus = 'PENDING_VALIDATION' | 'VERIFIED';
export type ApplicabilityStatus = 'PENDING_VALIDATION' | 'APPLICABLE' | 'NOT_APPLICABLE' | 'EXPIRED';

export interface EvaluationModel {id:string;name:string;description:string;createdBy:string;createdAt:string}
export interface ModelNode {id?:string;code:string;name:string;description:string;position:number;children:readonly ModelNode[];nodeType?:NodeType}
export interface ScaleOption {id?:string;code:string;label:string;position:number;declaredValue?:number}
export interface RatingScale {id?:string;code:string;name:string;options:readonly ScaleOption[]}
export interface ProgressFormula {code:string;schema:string;results:readonly string[];inputs:readonly string[];zeroPolicy:string;notApplicablePolicy:string;examples:readonly {input:Record<string,unknown>;expected:Record<string,unknown>}[];parameters:Record<string,unknown>}
export interface NormativeReference {id?:string;title:string;issuer?:string|null;edition?:string|null;documentReference:string;documentSha256?:string|null;pages?:string|null;disposition?:string|null;verificationStatus:VerificationStatus;verifiedBy?:string|null;verifiedAt?:string|null}
export interface RequiredSource {id?:string;indicatorNodeId:string;code:string;sourceType:'FILE'|'EXTERNAL_LINK'|'STRUCTURED_DATA'|'INSTITUTIONAL_REFERENCE';name:string;description:string;required:boolean;cardinalityMin:number;cardinalityMax?:number|null;periodRule:string;ordinal:number}
export interface IndicatorDefinition {nodeId:string;nature:'QUALITATIVE'|'QUANTITATIVE';standardText:string;fundamentalElements:readonly string[];evaluationPeriodDefinition:string;ratingScaleId?:string|null;valuationRuleId?:string|null;sources:readonly RequiredSource[];normativeReferences?:readonly NormativeReference[]}
export interface ValuationVariable {code:string;name:string;unit:string;dataType:'DECIMAL'|'INTEGER';required:boolean;validationDefinition:string}
export interface ValuationThreshold {ratingOptionId:string;lowerBound?:number|null;upperBound?:number|null;lowerInclusive:boolean;upperInclusive:boolean}
export interface ValuationRule {id?:string;indicatorNodeId:string;ruleType:'MANUAL_QUALITATIVE'|'QUANTITATIVE';methodDefinition:string;formulaDefinition?:string|null;strategyKey?:string|null;strategyVersion?:string|null;scaleId?:string|null;variables:readonly ValuationVariable[];thresholds:readonly ValuationThreshold[];missingDataPolicy:string;status:'DRAFT'|'VALIDATED'|'RETIRED'}
export interface ModelConfiguration {indicators:readonly IndicatorDefinition[];scales:readonly RatingScale[];rules:readonly ValuationRule[]}
export interface VersionDraft {source:string;criteria:readonly ModelNode[];scale:RatingScale|null;formula:ProgressFormula|null}
export interface ConfiguredVersionDraft {source:string;nodes:readonly ModelNode[];configuration:ModelConfiguration}
export interface ModelVersion {id:string;modelId:string;versionNumber:number;status:ModelStatus;source:string;authorId:string;approverId?:string;predecessorId?:string;revision:number;createdAt:string;publishedAt?:string;criteria?:readonly ModelNode[];scale?:RatingScale|null;formula?:ProgressFormula|null;configuration?:ModelConfiguration|null}
export interface ModelComparison {fromVersionId:string;toVersionId:string;changes:readonly {key:string;kind:'ADDED'|'REMOVED'|'MODIFIED'|'UNCHANGED'}[]}
export interface ApplicabilityInput {careerId:string;decision:Exclude<ApplicabilityStatus,'PENDING_VALIDATION'>;validFrom:string;validUntil?:string|null;provenance:{authorityFunction:string;legalBasis:string;documentReference:string;reason:string}}
export interface ModelCareerApplicability {id:string;modelVersionId:string;careerId:string;status:ApplicabilityStatus;validFrom:string;validUntil?:string|null;provenance:{authorityFunction:string;legalBasis:string;documentReference:string;reason:string};predecessorId?:string|null;revision:number;decidedBy:string;decidedAt:string}
