# TODOs: Exploration Topics & Technical Investigations

## Overview

This directory contains **exploratory technical investigations** and architectural patterns being considered for the Cheshire framework. These are **NOT** finalized decisions or commitments, but rather research topics, patterns, and technologies worth investigating.

## Purpose

- **Document potential architectural patterns** that could benefit Cheshire
- **Evaluate technologies and libraries** for possible integration
- **Explore performance optimization techniques**
- **Investigate security, observability, and scalability patterns**
- **Maintain a backlog of ideas** for future development

## Important Notes

⚠️ **These are exploration topics, not roadmap items**
- No timelines or commitments are implied
- Topics may be rejected, deferred, or heavily modified
- Some investigations may overlap or contradict each other
- Feasibility and priority are to be determined

## Document Categories

### Performance & Scalability
- `01-*-HANDLE_BIG_RESPONSE.MD` - Large payload handling strategies
- `BLOOM_FILTERS.MD` - Join optimization with probabilistic data structures
- `PROBABILISTIC_DATA_STRUCTURES.MD` - Space-efficient data structure patterns
- `DISTRIBUTED_COMPUTE_SERVICES.MD` - Distributed query execution

### Security
- `02-SECURITY.MD` - RBAC/ABAC with Cedar Policy integration

### Query Processing
- `03-CALCITE_AS_FEDERATOR.MD` - Apache Calcite for federated queries
- `DELEGATE_QE_TO_EXTENRNAL.MD` - External query engine integration
- `JOOG.MD` - jOOQ for type-safe SQL

### Reactive & Async
- `REACTOR.MD` - Project Reactor integration for observability
- `04-POSA_CONTEXT_PROPAGATION_PATTERS.md` - Context propagation patterns

### Control Plane
- `ARD_STASH_PATTTERN.MD` - Admission control with priority stash
- `CAP_SCHEDULER.MD` - Capability scheduling patterns
- `CONTROL_PLAN_DASHBOARD.MD` - Runtime monitoring dashboard
- `FLOW_ENGINE.MD` - Workflow orchestration

### Infrastructure
- `ENVOY.MD` - Service mesh integration
- `ODAP.MD` - Open Data Access Protocol

### Extensibility
- `SPI_EXTENSION.MD` - Runtime pluggable implementations (Python, Scala, in-memory compilation)

### Testing
- `JGIVEN_TESTING.MD` - BDD testing framework exploration

## How to Use

1. **Browse topics** to understand potential future directions
2. **Add new topics** as `.MD` files with clear problem statements
3. **Update topics** as research progresses or requirements change
4. **Move mature topics** to architecture docs when decisions are made

## Document Template

When adding new exploration topics, use this structure:

```markdown
# Topic: [Technology/Pattern Name]

## Problem Statement

What challenge or opportunity does this address?

## Potential Approaches

List of approaches being considered

## Benefits

- Expected advantages
- Use cases that would benefit

## Challenges

- Implementation complexity
- Integration concerns
- Performance considerations

## References

- Links to documentation
- Related projects
- Academic papers

## Next Steps

Concrete steps to evaluate feasibility
```

## Status

This directory is **actively maintained** but represents **exploratory work** only. Topics may be added, removed, or significantly changed without notice.

## Contributing

When adding or updating exploration topics:

- ✅ Be clear about the problem being solved
- ✅ List multiple approaches when applicable
- ✅ Include references and prior art
- ✅ Keep scope focused and specific
- ❌ Don't make commitments or promises
- ❌ Don't include specific timelines
- ❌ Don't make final architectural decisions

---

**Last Updated:** January 2026

