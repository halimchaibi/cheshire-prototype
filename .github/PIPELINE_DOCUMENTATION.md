# GitHub Actions CI/CD Pipeline Documentation

## Overview

The Cheshire Framework project includes a comprehensive CI/CD pipeline using GitHub Actions with all standard DevOps practices and automated workflows.

## Pipeline Structure

### 1. **CI Pipeline** (`ci.yml`)

**Trigger**: Push to `main`, `develop`, `release/**`, `hotfix/**` branches, and pull requests

**Jobs**:

- **build**: Main build and test job
  - Checkout code with full history
  - Set up JDK 21 with Maven caching
  - Validate Maven wrapper
  - Build project and run tests
  - Generate and publish test reports
  - Upload test results as artifacts

- **code-quality**: Code quality analysis
  - Run Checkstyle for code style compliance
  - Run SpotBugs for bug detection
  - Generate code coverage with JaCoCo
  - Upload coverage to Codecov
  - Store quality reports as artifacts

- **dependency-check**: Security vulnerability scanning
  - Analyze dependencies
  - Run OWASP dependency check
  - Generate vulnerability reports

- **build-matrix**: Multi-platform build testing
  - Test on Ubuntu, Windows, and macOS
  - Ensure cross-platform compatibility

- **package**: Create distribution artifacts
  - Package JARs
  - Create tar.gz and zip archives
  - Upload artifacts for deployment

- **docker-build**: Build and push Docker images
  - Multi-arch builds (amd64, arm64)
  - Push to Docker Hub and GitHub Container Registry
  - Tag with branch name, SHA, and latest

- **notify**: Build status notifications
  - Send Slack notifications on failures

### 2. **Release Pipeline** (`release.yml`)

**Trigger**: Git tags matching `v*.*.*` pattern or manual workflow dispatch

**Jobs**:

- **prepare-release**: Version management
  - Extract and validate version from tag
  - Ensure semantic versioning compliance

- **build-release**: Build release artifacts
  - Update POM versions
  - Build and test project
  - Generate source and javadoc JARs
  - Sign artifacts with GPG
  - Create distribution archives
  - Generate checksums

- **create-github-release**: GitHub Release creation
  - Generate release notes from commits
  - Create GitHub release
  - Upload artifacts
  - Mark as prerelease if version contains qualifier

- **publish-maven-central**: Maven Central deployment
  - Deploy signed artifacts to OSSRH
  - Publish to Maven Central

- **publish-docker**: Docker image release
  - Build multi-arch images
  - Push to Docker Hub and GHCR
  - Tag with semantic versioning (major, major.minor, major.minor.patch)

- **notify-release**: Release notifications
  - Send Slack notifications with release details

### 3. **Pull Request Validation** (`pr-validation.yml`)

**Trigger**: PR opened, synchronized, or reopened (non-draft)

**Jobs**:

- **pr-checks**: Initial PR validation
  - Check PR title follows conventional commits
  - Detect merge conflicts
  - Check for large files

- **code-validation**: Code quality checks
  - Validate POM files
  - Check code formatting
  - Run Checkstyle
  - Detect TODO/FIXME comments

- **build-and-test**: Build and test PR changes
  - Build project
  - Run unit and integration tests
  - Generate coverage report
  - Comment coverage on PR

- **diff-analysis**: Change analysis
  - List changed files
  - Count lines added/deleted
  - Flag API changes

- **dependency-review**: Dependency scanning
  - Review new dependencies
  - Check for vulnerable or prohibited licenses

- **security-scan**: Security analysis
  - OWASP dependency check
  - Trivy vulnerability scanning
  - Upload results to GitHub Security

- **performance-check**: Performance validation
  - Run performance tests
  - Check build time

- **pr-summary**: Generate PR summary
  - Create comment with validation results
  - Show job statuses

- **auto-label**: Automatic PR labeling
  - Label based on changed files

### 4. **Dependency Update** (`dependency-update.yml`)

**Trigger**: Weekly on Monday at 9:00 AM UTC or manual dispatch

**Jobs**:

- **update-dependencies**: Automated dependency updates
  - Check for dependency updates
  - Update to latest releases (no major versions)
  - Update Maven plugins
  - Test with updated dependencies
  - Create PR with changes

- **dependabot-auto-merge**: Auto-merge Dependabot PRs
  - Automatically merge minor and patch updates
  - After CI validation passes

### 5. **CodeQL Security Analysis** (`codeql.yml`)

**Trigger**: Push to main/develop, PRs, weekly on Sunday at 3:00 AM UTC

**Jobs**:

- **analyze**: Static code analysis
  - Initialize CodeQL
  - Build project
  - Perform security analysis
  - Upload results to GitHub Security

## Configuration Files

### Dependabot (`dependabot.yml`)

- Automated dependency updates for Maven and GitHub Actions
- Weekly schedule on Mondays
- Grouped updates for related dependencies
- Auto-reviewers and labels

### Labeler (`labeler.yml`)

- Automatic PR labeling based on changed files
- Labels for: documentation, java, configuration, tests, build, ci, modules

## Required Secrets

Configure these secrets in GitHub repository settings:

### Docker
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password/token

### Maven Central
- `OSSRH_USERNAME`: Sonatype OSSRH username
- `OSSRH_PASSWORD`: Sonatype OSSRH password
- `GPG_PRIVATE_KEY`: GPG private key for signing
- `GPG_PASSPHRASE`: GPG key passphrase

### Code Coverage
- `CODECOV_TOKEN`: Codecov upload token

### Notifications
- `SLACK_WEBHOOK_URL`: Slack webhook for notifications

## Workflow Features

### ✅ Standard CI/CD Practices

1. **Automated Testing**
   - Unit tests
   - Integration tests
   - Performance tests
   - Multi-platform testing

2. **Code Quality**
   - Checkstyle enforcement
   - SpotBugs analysis
   - Code coverage tracking
   - Test result reporting

3. **Security**
   - Dependency vulnerability scanning (OWASP)
   - Container scanning (Trivy)
   - CodeQL static analysis
   - License compliance checking

4. **Build Optimization**
   - Maven dependency caching
   - Docker layer caching
   - Parallel job execution
   - Conditional job execution

5. **Artifact Management**
   - JAR packaging
   - Source and javadoc generation
   - GPG signing
   - Checksum generation
   - Retention policies

6. **Release Management**
   - Semantic versioning
   - Automated changelog generation
   - GitHub Releases
   - Maven Central publishing
   - Docker image publishing

7. **Pull Request Workflow**
   - Conventional commit validation
   - Code review automation
   - Coverage reporting
   - Security checks
   - Auto-labeling

8. **Dependency Management**
   - Automated updates
   - Dependency review
   - Auto-merge safe updates
   - Grouped dependency updates

## Usage

### Triggering CI Pipeline

```bash
# Push to main or develop branch
git push origin main

# Create pull request
gh pr create --base main --head feature-branch
```

### Creating a Release

```bash
# Tag a release
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Or trigger manually
gh workflow run release.yml -f version=1.0.0
```

### Running Workflows Manually

```bash
# Trigger CI manually
gh workflow run ci.yml

# Trigger dependency update
gh workflow run dependency-update.yml
```

### Viewing Workflow Status

```bash
# List workflow runs
gh run list

# View specific run
gh run view <run-id>

# View logs
gh run view <run-id> --log
```

## Best Practices

1. **Commit Messages**: Follow conventional commits format
   - `feat:` for new features
   - `fix:` for bug fixes
   - `docs:` for documentation
   - `chore:` for maintenance

2. **Pull Requests**:
   - Keep PRs focused and small
   - Ensure all checks pass before merging
   - Address code review feedback
   - Maintain test coverage

3. **Releases**:
   - Use semantic versioning
   - Update CHANGELOG.md
   - Test thoroughly before tagging
   - Write meaningful release notes

4. **Security**:
   - Review dependency updates
   - Monitor security alerts
   - Keep dependencies up to date
   - Rotate secrets regularly

## Monitoring and Maintenance

- Check GitHub Actions dashboard regularly
- Review failed workflows and fix issues
- Monitor code coverage trends
- Keep workflows updated with latest action versions
- Review and update secrets periodically

## Troubleshooting

### Build Failures

1. Check error logs in Actions tab
2. Reproduce locally with `./mvnw clean verify`
3. Check for dependency issues
4. Verify Java version compatibility

### Docker Build Issues

1. Test Dockerfile locally: `docker build -t cheshire:test .`
2. Check Docker Hub credentials
3. Verify multi-arch build support

### Release Failures

1. Verify GPG key configuration
2. Check Maven Central credentials
3. Ensure version numbers are valid
4. Verify tag format matches pattern

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven CI/CD Best Practices](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html)
- [Semantic Versioning](https://semver.org/)
- [Conventional Commits](https://www.conventionalcommits.org/)

