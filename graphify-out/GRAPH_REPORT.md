# Graph Report - sec-unl  (2026-10-17)

## Corpus Check
- 68 files · ~68,262 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 11 file(s) not represented in the graph (top: .toml 10, (none) 1)

## Summary
- 97 nodes · 238 edges · 7 communities (6 shown, 1 thin omitted)
- Extraction: 76% EXTRACTED · 24% INFERRED · 0% AMBIGUOUS · INFERRED: 57 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Project Skills & Spec Kit Core
- Spec Kit Commands & Templates
- PowerShell Spec Kit Scripts
- Harness Skill Definitions
- Branch Name Generation (PS)
- SEC-UNL Documentation Site

## God Nodes (most connected - your core abstractions)
1. `Project Constitution (.specify/memory/constitution.md)` - 18 edges
2. `Project Constitution (.specify/memory/constitution.md)` - 18 edges
3. `Spec Kit Core Artifacts (spec.md, plan.md, tasks.md)` - 17 edges
4. `speckit.tasks command (dependency-ordered task generation)` - 12 edges
5. `speckit-specify Skill (.claude harness)` - 11 edges
6. `speckit.implement command (task execution)` - 10 edges
7. `speckit.plan command (implementation planning)` - 10 edges
8. `speckit.specify command (feature specification creation)` - 10 edges
9. `Resolve-TemplateContent()` - 9 edges
10. `speckit-analyze Command` - 9 edges

## Surprising Connections (you probably didn't know these)
- `speckit-analyze Skill (.claude harness)` --semantically_similar_to--> `speckit.analyze Command (.opencode harness)`  [INFERRED] [semantically similar]
  .claude/skills/speckit-analyze/SKILL.md → .opencode/commands/speckit.analyze.md
- `speckit-checklist Skill (.claude harness)` --semantically_similar_to--> `speckit.checklist Command (.opencode harness)`  [INFERRED] [semantically similar]
  .claude/skills/speckit-checklist/SKILL.md → .opencode/commands/speckit.checklist.md
- `speckit-clarify Skill (.claude harness)` --semantically_similar_to--> `speckit.clarify Command (.opencode harness)`  [INFERRED] [semantically similar]
  .claude/skills/speckit-clarify/SKILL.md → .opencode/commands/speckit.clarify.md
- `speckit-specify Skill (.agents harness)` --semantically_similar_to--> `speckit-specify Skill (.claude harness)`  [INFERRED] [semantically similar]
  .agents/skills/speckit-specify/SKILL.md → .claude/skills/speckit-specify/SKILL.md
- `speckit-tasks Skill (.agents harness)` --semantically_similar_to--> `speckit-tasks Skill (.claude harness)`  [INFERRED] [semantically similar]
  .agents/skills/speckit-tasks/SKILL.md → .claude/skills/speckit-tasks/SKILL.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **SEC-UNL Specialized Skill Template Family** — _agents_skills_angular_frontend_skill_angular_frontend, _agents_skills_c4_diagrams_skill_c4_diagrams, _agents_skills_graphify_mapping_skill_graphify_mapping, _agents_skills_kotlin_android_skill_kotlin_android, _agents_skills_multiagent_coordination_skill_multiagent_coordination, _agents_skills_openapi_contracts_skill_openapi_contracts, _agents_skills_postgresql_database_skill_postgresql_database, _agents_skills_quarto_documentation_skill_quarto_documentation, _agents_skills_sdd_requirements_skill_sdd_requirements, _agents_skills_security_rbac_skill_security_rbac, _agents_skills_software_testing_skill_software_testing [INFERRED 0.95]
- **Spec Kit Development Workflow Command Suite** — _agents_skills_speckit_constitution_skill_speckit_constitution, _agents_skills_speckit_clarify_skill_speckit_clarify, _agents_skills_speckit_plan_skill_speckit_plan, _agents_skills_speckit_analyze_skill_speckit_analyze, _agents_skills_speckit_checklist_skill_speckit_checklist, _agents_skills_speckit_implement_skill_speckit_implement, _agents_skills_speckit_converge_skill_speckit_converge, _agents_skills_speckit_constitution_skill_project_constitution, _agents_skills_speckit_analyze_skill_core_artifacts [INFERRED 0.85]
- **Speckit Pre/Post Extension Hooks Protocol** — _agents_skills_speckit_analyze_skill_speckit_analyze, _agents_skills_speckit_checklist_skill_speckit_checklist, _agents_skills_speckit_clarify_skill_speckit_clarify, _agents_skills_speckit_constitution_skill_speckit_constitution, _agents_skills_speckit_converge_skill_speckit_converge, _agents_skills_speckit_implement_skill_speckit_implement, _agents_skills_speckit_plan_skill_speckit_plan, _agents_skills_speckit_analyze_skill_extension_hooks_protocol [EXTRACTED 1.00]
- **Spec Kit feature-development pipeline (.claude harness): specify → clarify → plan → tasks → implement → analyze → converge** — _claude_skills_speckit_specify_skill_speckit_specify, _claude_skills_speckit_clarify_skill_speckit_clarify, _claude_skills_speckit_plan_skill_speckit_plan, _claude_skills_speckit_tasks_skill_speckit_tasks, _claude_skills_speckit_implement_skill_speckit_implement, _claude_skills_speckit_analyze_skill_speckit_analyze, _claude_skills_speckit_converge_skill_speckit_converge [INFERRED 0.85]
- **Pre/Post Extension Hook Protocol participants (.specify/extensions.yml)** — _claude_skills_speckit_specify_skill_extension_hooks, _agents_skills_speckit_specify_skill_speckit_specify, _agents_skills_speckit_tasks_skill_speckit_tasks, _agents_skills_speckit_taskstoissues_skill_speckit_taskstoissues, _claude_skills_speckit_analyze_skill_speckit_analyze, _claude_skills_speckit_checklist_skill_speckit_checklist, _claude_skills_speckit_clarify_skill_speckit_clarify, _claude_skills_speckit_constitution_skill_speckit_constitution, _claude_skills_speckit_converge_skill_speckit_converge, _claude_skills_speckit_implement_skill_speckit_implement, _claude_skills_speckit_plan_skill_speckit_plan, _claude_skills_speckit_specify_skill_speckit_specify, _claude_skills_speckit_tasks_skill_speckit_tasks, _claude_skills_speckit_taskstoissues_skill_speckit_taskstoissues, _opencode_commands_speckit_analyze_speckit_analyze, _opencode_commands_speckit_checklist_speckit_checklist, _opencode_commands_speckit_clarify_speckit_clarify, _opencode_commands_speckit_constitution_speckit_constitution [EXTRACTED 1.00]
- **Commands governed by the project constitution (.specify/memory/constitution.md)** — _claude_skills_speckit_constitution_skill_speckit_constitution, _claude_skills_speckit_specify_skill_speckit_specify, _claude_skills_speckit_clarify_skill_speckit_clarify, _claude_skills_speckit_plan_skill_speckit_plan, _claude_skills_speckit_tasks_skill_speckit_tasks, _claude_skills_speckit_analyze_skill_speckit_analyze, _claude_skills_speckit_converge_skill_speckit_converge, _claude_skills_speckit_implement_skill_speckit_implement, _claude_skills_speckit_checklist_skill_speckit_checklist, _claude_skills_speckit_taskstoissues_skill_speckit_taskstoissues, _agents_skills_speckit_specify_skill_speckit_specify, _agents_skills_speckit_tasks_skill_speckit_tasks, _agents_skills_speckit_taskstoissues_skill_speckit_taskstoissues, _agents_skills_spring_boot_layered_skill_spring_boot_layered, _opencode_commands_speckit_analyze_speckit_analyze, _opencode_commands_speckit_checklist_speckit_checklist, _opencode_commands_speckit_clarify_speckit_clarify, _opencode_commands_speckit_constitution_speckit_constitution [INFERRED 0.95]
- **Full SDD Cycle: specify -> review gate -> plan -> review gate -> tasks -> implement** — _specify_workflows_speckit_workflow_speckit_workflow, _specify_workflows_speckit_workflow_review_gates, _opencode_commands_speckit_specify_speckit_specify, _opencode_commands_speckit_plan_speckit_plan, _opencode_commands_speckit_tasks_speckit_tasks, _opencode_commands_speckit_implement_speckit_implement [EXTRACTED 1.00]
- **All speckit commands load the project constitution as non-negotiable governance constraints** — _specify_memory_constitution_constitution, _opencode_commands_speckit_specify_speckit_specify, _opencode_commands_speckit_plan_speckit_plan, _opencode_commands_speckit_tasks_speckit_tasks, _opencode_commands_speckit_implement_speckit_implement, _opencode_commands_speckit_converge_speckit_converge, _opencode_commands_speckit_taskstoissues_speckit_taskstoissues [EXTRACTED 1.00]
- **Quarto SEC-UNL documentation site (home + technical docs + user manual)** — _quarto_quarto, index_index, docs_technical_index_index, docs_user_index_index [EXTRACTED 1.00]

## Communities (7 total, 1 thin omitted)

### Community 0 - "Project Skills & Spec Kit Core"
Cohesion: 0.21
Nodes (22): angular-frontend Skill, c4-diagrams Skill, graphify-mapping Skill, kotlin-android Skill, multiagent-coordination Skill, openapi-contracts Skill, postgresql-database Skill, quarto-documentation Skill (+14 more)

### Community 1 - "Spec Kit Commands & Templates"
Cohesion: 0.25
Nodes (16): speckit.analyze Command (.opencode harness), speckit.checklist Command (.opencode harness), speckit.clarify Command (.opencode harness), speckit.converge command (spec-to-code gap closure), speckit.implement command (task execution), speckit.plan command (implementation planning), speckit.specify command (feature specification creation), speckit.tasks command (dependency-ordered task generation) (+8 more)

### Community 2 - "PowerShell Spec Kit Scripts"
Cohesion: 0.23
Nodes (15): Find-SpecifyRoot(), Format-SpecKitCommand(), Get-CurrentBranch(), Get-FeaturePathsEnv(), Get-InvokeSeparator(), Get-NormalizedPriority(), Get-Python3Command(), Get-RepoRoot() (+7 more)

### Community 3 - "Harness Skill Definitions"
Cohesion: 0.35
Nodes (16): speckit-specify Skill (.agents harness), speckit-tasks Skill (.agents harness), speckit-taskstoissues Skill (.agents harness, deprecated), spring-boot-layered Skill (SEC-UNL), speckit-analyze Skill (.claude harness), speckit-checklist Skill (.claude harness), speckit-clarify Skill (.claude harness), Project Constitution (.specify/memory/constitution.md) (+8 more)

### Community 4 - "Branch Name Generation (PS)"
Cohesion: 0.39
Nodes (4): ConvertTo-AsciiLower(), ConvertTo-CleanBranchName(), ConvertTo-UnicodeWords(), Get-BranchName()

### Community 5 - "SEC-UNL Documentation Site"
Cohesion: 0.40
Nodes (5): Quarto documentation website configuration, Technical documentation section index, User manual section index, SEC-UNL documentation home page (index.qmd), SEC-UNL - Sistema de Evaluacion de Carreras (project)

## Ambiguous Edges - Review These
- `speckit-checklist Command` → `speckit-clarify Command`  [AMBIGUOUS]
  .agents/skills/speckit-clarify/SKILL.md · relation: shares_data_with

## Knowledge Gaps
- **4 isolated node(s):** `Gap Type Classification (missing/partial/contradicts/unrequested)`, `Constitution template`, `User manual section index`, `SEC-UNL - Sistema de Evaluacion de Carreras (project)`
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 11 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Project Constitution (.specify/memory/constitution.md)` connect `Harness Skill Definitions` to `Spec Kit Commands & Templates`?**
  _High betweenness centrality (0.237) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `Spec Kit Core Artifacts (spec.md, plan.md, tasks.md)` (e.g. with `angular-frontend Skill` and `c4-diagrams Skill`) actually correct?**
  _`Spec Kit Core Artifacts (spec.md, plan.md, tasks.md)` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Gap Type Classification (missing/partial/contradicts/unrequested)`, `Constitution template`, `User manual section index` to the rest of the system?**
  _4 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Why does `speckit-specify Skill (.agents harness)` connect `Harness Skill Definitions` to `Project Skills & Spec Kit Core`?**
  _High betweenness centrality (0.114) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `speckit-specify Skill (.claude harness)` (e.g. with `speckit-specify Skill (.agents harness)` and `Unit Tests for English (Requirements-Quality Checklists)`) actually correct?**
  _`speckit-specify Skill (.claude harness)` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Why does `speckit.analyze Command (.opencode harness)` connect `Spec Kit Commands & Templates` to `Harness Skill Definitions`?**
  _High betweenness centrality (0.101) - this node is a cross-community bridge._

### Low-confidence Hints
_AMBIGUOUS edges — the extractor was unsure. Verify before acting on these._

- **What is the exact relationship between `speckit-checklist Command` and `speckit-clarify Command`?**
  _Edge tagged AMBIGUOUS (relation: shares_data_with) - confidence is low._