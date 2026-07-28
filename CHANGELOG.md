# Changelog

All notable changes to Airspace Ops Simulator are documented here.

## [1.0.2] - 2026-07-28

### Security

- Updated PMD from 7.15.0 to 7.22.0, resolving reported vulnerabilities in
  the transitive PMD core and Apache Commons Lang tooling dependencies.

## [1.0.1] - 2026-07-27

### Changed

- Updated the Gradle wrapper from 8.14.2 to 9.6.1 after successful local and hosted verification.
- Normalized the Windows wrapper script to the repository's declared line-ending policy.

## [1.0.0] - 2026-07-27

### Added

- Configurable JavaFX airport-operations workspace.
- Concurrent flight-request production, dispatch, movement, and servicing pipelines.
- Thread-safe aircraft, airport, queue, and lifecycle coordination.
- Live operational counters and event logging.
- Gradle build conventions, PMD quality checks, and GitHub Actions verification.
- Architecture, operations-flow, and repository overview visuals.

### Changed

- Normalized the codebase under the `io.github.himath2002.airspace` namespace.
- Replaced legacy naming and excessive commentary with focused engineering documentation.
- Strengthened shutdown handling, queue dispatch, resource validation, and cross-thread state visibility.
- Applied icon scaling that was previously stored but not reflected by the renderer.
