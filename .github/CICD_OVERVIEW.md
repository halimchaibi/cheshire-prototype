# GitHub Configuration

This directory contains all GitHub-specific configurations for the Cheshire Framework project.

## Contents

### Workflows (`.github/workflows/`)

| Workflow                | Trigger                   | Purpose                                           |
|-------------------------|---------------------------|---------------------------------------------------|
| `ci.yml`                | Push to main/develop, PRs | Main CI pipeline with build, test, quality checks |
| `release.yml`           | Git tags `v*.*.*`         | Release automation and artifact publishing        |
| `pr-validation.yml`     | Pull requests             | PR validation with security and quality checks    |
| `dependency-update.yml` | Weekly (Monday)           | Automated dependency updates                      |
| `codeql.yml`            | Push, PR, Weekly          | Security code scanning                            |

### Configuration Files

- **`dependabot.yml`**: Automated dependency update configuration
- **`labeler.yml`**: Automatic PR labeling rules
- **`PIPELINE_DOCUMENTATION.md`**: Complete pipeline documentation

## Quick Start

### 1. Configure Secrets

Add these secrets in repository settings:

```
# Docker
DOCKER_USERNAME
DOCKER_PASSWORD

# Maven Central (optional)
OSSRH_USERNAME
OSSRH_PASSWORD
GPG_PRIVATE_KEY
GPG_PASSPHRASE

# Code Coverage (optional)
CODECOV_TOKEN

# Notifications (optional)
SLACK_WEBHOOK_URL
```

### 2. Enable Workflows

All workflows are enabled by default when pushed to GitHub.

### 3. Test Locally

```bash
# Install act (optional - for local testing)
brew install act  # macOS
# or
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run workflows locally
act -j build
act -j test
```

## Workflow Features

### ✅ Continuous Integration

- Build on multiple platforms (Linux, Windows, macOS)
- Java 21 with preview features
- Maven dependency caching
- Test execution and reporting
- Code coverage tracking

### ✅ Code Quality

- Checkstyle enforcement
- SpotBugs analysis
- JaCoCo code coverage
- Test result reporting

### ✅ Security

- OWASP dependency scanning
- Trivy vulnerability scanning
- CodeQL static analysis
- License compliance checking

### ✅ Release Management

- Semantic versioning
- Automated GitHub releases
- Maven Central publishing
- Docker image publishing (multi-arch)

### ✅ Pull Request Automation

- Conventional commit validation
- Code coverage reporting
- Security scanning
- Auto-labeling
- PR summary generation

### ✅ Dependency Management

- Weekly automated updates
- Dependabot integration
- Auto-merge safe updates
- Grouped dependency updates

## Badge Status

Add these badges to your README.md:

```markdown
[![CI](https://github.com/halimchaibi/cheshire-prototype/actions/workflows/ci.yml/badge.svg)](https://github.com/halimchaibi/cheshire-prototype/actions/workflows/ci.yml)
[![CodeQL](https://github.com/halimchaibi/cheshire-prototype/actions/workflows/codeql.yml/badge.svg)](https://github.com/halimchaibi/cheshire-prototype/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/halimchaibi/cheshire-prototype/branch/main/graph/badge.svg)](https://codecov.io/gh/halimchaibi/cheshire-prototype)
```

## Customization

### Modify Workflow Triggers

Edit the `on:` section in workflow files:

```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
```

### Add Custom Jobs

Add jobs to existing workflows:

```yaml
jobs:
  my-custom-job:
    name: My Custom Job
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "Custom step"
```

### Configure Notifications

Update notification steps in workflows to use your preferred platform (Slack, Discord, Email, etc.).

## Maintenance

- Review and update action versions quarterly
- Rotate secrets annually
- Monitor workflow execution times
- Optimize caching strategies
- Keep documentation current

## Support

For issues or questions about the CI/CD pipeline:

1. Check [PIPELINE_DOCUMENTATION.md](PIPELINE_DOCUMENTATION.md)
2. Review workflow run logs
3. Create an issue with the `ci` label

