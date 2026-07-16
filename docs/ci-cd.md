# CI/CD

## Workflows

- `CI`: runs the complete Maven test suite, including Testcontainers, generates JaCoCo coverage, submits the analysis to SonarQube, waits for the Quality Gate, and uploads the executable JAR. It runs for pull requests and pushes to `main`.
- `Security`: rejects new dependencies with known high or critical vulnerabilities and runs CodeQL for Java. CodeQL also runs weekly.
- `Release`: after a successful `CI` run on `main`, builds the OCI image and publishes `latest` and the commit SHA to `ghcr.io/<owner>/<repository>`. Publishing an immutable artifact is not a production deployment and therefore does not use the `production` environment approval.

The proposed production flow, including staging validation, protected promotion, canary traffic, automatic SLO evaluation, and rollback, is documented in [Canary deployment proposal](canary-deployment-proposal.md).

Dependabot checks Maven dependencies, GitHub Actions, and the Docker base images every Monday.

## Repository configuration

Configure the `main` branch ruleset to require these checks before merging:

- `Build, test and quality gate`
- `Dependency review`
- `CodeQL`

Configure the SonarQube project with:

- repository variable `SONAR_HOST_URL`: externally reachable SonarQube URL;
- repository variable `SONAR_PROJECT_KEY`: the project key configured in SonarQube;
- repository secret `SONAR_TOKEN`: project analysis token with Execute Analysis permission.

The SonarQube server must be reachable from the GitHub runner. A server exposed only at `localhost` or on a private network requires a self-hosted runner or private network connectivity. Pull request analysis requires a SonarQube edition that supports that feature; fork pull requests run tests but skip SonarQube because GitHub does not expose repository secrets to them.

Create the `production` environment with required reviewers for the production deployment job. Image publication uses only the repository-scoped `GITHUB_TOKEN`; no long-lived registry credential is required.

For private repositories, Dependency Review and CodeQL availability depend on the GitHub security features enabled for the organization. Enable the dependency graph and GitHub Advanced Security where required.

## Deployment target

The release workflow produces an immutable image identified by the full Git commit SHA. A runtime deployment should reference that SHA tag instead of `latest`:

```text
ghcr.io/<owner>/<repository>:<commit-sha>
```

Connect the final deployment to the selected platform (for example ECS, Kubernetes, or Cloud Run) as a separate job protected by a GitHub environment. Keep cloud authentication passwordless through GitHub OIDC and avoid repository secrets with long-lived access keys.
