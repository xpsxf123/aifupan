# Skills Index

## Conventions

- Each skill must define inputs, outputs, boundaries, and failure handling
- Templates belong to `skills/<skill_name>/templates/`
- Register new skills here and link them from the agent `INDEX.md` when needed

## Groups (suggested)

- Deliverables: frontend docs / prototypes / diagrams
- Analysis: UI structure / dependency / data model / permissions
- Validation: form validation rules
- Orchestration: combine analysis skills for implementation planning
- Governance: writeback / WAL / skill index / quality gates

## Available Skills

- frontend_requirement_doc_generator: generate frontend requirement docs
- prototype_layout_data_generator: generate layout/prototype structured data
- prototype_page_preview: preview prototype pages
- interaction_flow_analyzer: analyze interaction flows
- page_structure_analyzer: analyze page structure and hierarchy
- layout_structure_analyzer: analyze layout structure and sections
- api_dependency_analyzer: analyze API dependency graph
- data_model_analyzer: analyze data model usage
- permission_rule_analyzer: analyze permission rules
- state_sharing_analyzer: analyze shared state strategy
- form_validation_analyzer: analyze form validation rules
- mindmap_preview: preview mindmaps
- wiki_graph_builder: build wiki knowledge graph and update INDEX/wiki writeback plans
- memory_compressor: compress high-value stable knowledge into MEMORY.md summaries
- legacy_wiki_bootstrapper: bootstrap INDEX/wiki/MEMORY from legacy projects and produce gap list
- demand_version_manager: manage versioned requirement/API docs in docs/需求版本库 and distill stable pointers into MEMORY.md
- task_decomposition_guide: decompose large tasks into testable subtasks with acceptance criteria
- spec_quality_checklist: validate specs and outputs before implementation and delivery
- skill_graph_manager: maintain skills index and cross-links to keep skills discoverable
- wal_documentation_rules: standardize WAL fragments and writeback rules to prevent wiki bloat

## JSON-driven Pipeline Skills

- project_scaffolder: scaffold frontend project from global config
- router_generator: generate routing and navigation
- layout_framework_generator: generate global layout framework
- component_abstractor: extract reusable components
- page_code_generator: generate page code from component tree and bindings
- form_validation_binder: bind JSON validations to frontend schemas
- api_integration_mapper: map pages/features to OpenAPI endpoints and propose integration + smoke scope
- api_client_generator: generate typed API clients and hooks
- action_binder: bind action definitions to handler code
- state_management_generator: generate shared state management
- mock_api_generator: generate mock server/data from API definitions
- dynamic_mock_backend: generate dynamic Node.js mock backend plan with JSON file persistence (DB-like)
- mock_tester_page: generate local HTML tester page (searchable endpoints + code/table view + pagination + CRUD + auto-seed)
- style_integrator: integrate design tokens and style variables
- build_config_generator: generate build and environment configs
