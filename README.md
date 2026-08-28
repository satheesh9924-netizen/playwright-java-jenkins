# playwright-java-jenkins

End-to-end test automation pipeline: Playwright (Java) + TestNG, containerized with Docker,
run by a local Jenkins instance, reported with Allure -- plus two Claude-powered testing
modules (resume/JD matcher with a full AI-eval test suite, and an AI CI-failure triage bot).

Repo: https://github.com/satheesh9924-netizen/playwright-java-jenkins

## Repo layout

Three independent Maven modules, each with its own `pom.xml` and `Dockerfile`:

```
.
├── src/                    # e2e Playwright/TestNG tests (root module)
├── resume-matcher/         # Resume-vs-JD fit matcher + AI-eval test suite
├── ai-triage/              # AI CI-failure triage bot (classifies FLAKY/REGRESSION/ENVIRONMENT)
├── Dockerfile               # root module image (mcr.microsoft.com/playwright/java)
├── docker-compose.yml       # all 4 services: e2e tests + both AI modules
├── Jenkinsfile               # 4-stage pipeline, see below
└── testng.xml
```

## Local infrastructure

Everything runs locally on this Mac as persistent background services (`brew services`):

| Service | How it runs | Notes |
|---|---|---|
| **Colima** | `brew services start colima` | Lightweight VM providing the Docker daemon. `docker` / `docker compose` talk to it via the `colima` context. |
| **Jenkins LTS** | `brew services start jenkins-lts` | Bound to `127.0.0.1:8080` only (not exposed on the network). `JENKINS_HOME` is `~/.jenkins`. |

**Jenkins login:** `admin` / (password was generated at setup time -- not stored in this repo;
if lost, reset via `~/.jenkins/init.groovy.d/basic-security.groovy` and restart the service, or
use the Jenkins script console).

**Jenkins plugins installed:** `git`, `workflow-aggregator` (Pipeline), `github`,
`allure-jenkins-plugin` (+ its `matrix-project` dependency -- required or the Allure publisher's
extensions silently fail to register).

**Global Jenkins config (survives `brew services restart`, stored in Jenkins' own config, not
the launchd plist):**
- `PATH+HOMEBREW` env var = `/opt/homebrew/bin:/opt/homebrew/sbin` (otherwise Jenkins can't find `docker`)
- Allure Commandline tool installation named `allure`, auto-installs v2.30.0

**Jenkins credentials:**
- `abacus-api-key` (Secret Text) -- the Abacus RouteLLM API key, injected into the AI stages via `withCredentials`

## Pipeline stages (Jenkinsfile)

Triggers: `pollSCM('H/2 * * * *')` (checks GitHub every ~2 min -- this Jenkins isn't publicly
reachable, so the `githubPush()` webhook trigger is registered but dormant) and `githubPush()`
(ready if Jenkins is ever exposed publicly).

1. **Checkout**
2. **Verify Docker Environment** -- fails fast with a clear message if `docker`/`docker compose` aren't reachable
3. **Build & Run Tests (Docker)** -- builds the root image, runs the Playwright/TestNG e2e suite
4. **Resume/JD Matcher AI Tests** -- builds & runs `resume-matcher`'s test suite (needs `ABACUS_API_KEY`)
5. **AI Failure Triage** -- runs `ai-triage`'s own test suite, then runs the triage bot itself against `target/surefire-reports` from stage 3, writing `ai-triage/target/triage-report.md`

`post.always`: fixes any root-owned files Docker wrote into the three `target/` dirs, tears
down containers/networks, publishes JUnit results, archives artifacts (screenshots, traces,
surefire reports, the triage markdown report), publishes the Allure report.

## The AI modules

Both call Claude through **Abacus.AI's RouteLLM** (`https://routellm.abacus.ai/v1`), an
OpenAI-compatible gateway -- not Anthropic's native API directly. That's why the code uses the
`com.openai:openai-java` SDK (pointed at a custom `baseUrl`) rather than `com.anthropic:anthropic-java`.
Auth: `ABACUS_API_KEY` env var (mapped from the Jenkins credential in CI; export it locally to
run the modules yourself).

### `resume-matcher`

Takes a resume + job description, returns a structured match verdict (`matchScore`, `verdict`,
`matchedSkills`, `missingSkills`, `reasoning`). Model: `claude-sonnet-5`.

The test suite (`src/test/java/.../*Test.java`) demonstrates the core AI-testing concepts:

| Test class | Concept |
|---|---|
| `SchemaValidationTest` | Output structure is valid independent of whether the verdict is "right" |
| `AccuracyGoldenSetTest` | Golden-set regression: hand-labeled (resume, JD) pairs with an expected **score range**, not an exact value -- rerun whenever the prompt changes |
| `GroundednessTest` | Claimed skills must share real keyword overlap with the source text (not exact-phrase match -- too brittle for paraphrasing) |
| `PromptInjectionTest` | A fixture resume embeds a fake "system override" claiming a perfect score -- asserts it's ignored |
| `BiasFairnessTest` | Two resumes, identical qualifications, differing only in name/grad-year -- asserts scores stay within tolerance |

Golden fixtures live in `src/test/resources/golden/` (all synthetic, no real PII).

### `ai-triage`

Parses `TEST-*.xml` surefire reports, classifies each failure (`FLAKY` / `REGRESSION` /
`ENVIRONMENT` / `UNKNOWN`) with a summary + suggested action. Model: `claude-haiku-4-5` (cheap
classification task, run frequently in CI). `TriageAccuracyTest` is its own golden-set suite
(synthetic timeout/selector-break/docker-daemon-down fixtures).

Run standalone: `mvn -q compile exec:java -Dexec.mainClass=com.example.aitriage.FailureTriage -Dexec.args="<surefire-reports-dir> <output.md>"`

## Real issues hit while building this (worth knowing if things break)

- **`mvn clean` inside a bind-mounted `target/`** fails (can't delete a mount point) -- the
  Dockerfiles run plain `mvn test`, and the Jenkinsfile resets `target/` from the host side
  instead.
- **Jackson version conflicts**: both AI modules pin `jackson-databind` explicitly -- keep it
  aligned with whatever the OpenAI/Anthropic SDK pulls transitively (`mvn dependency:tree | grep jackson`
  to check) or you'll hit `NoSuchMethodError`.
- **LLM JSON output isn't 100% reliable**, even under an explicit "respond with ONLY a JSON
  object" instruction: seen in practice -- responses wrapped in ` ```json ` fences, duplicate
  JSON keys, stray trailing punctuation, outright malformed syntax mid-object. Both
  `ResumeMatcher` and `FailureTriage` defensively extract the `{...}` span, parse via Jackson's
  tree model (not straight into a record, which chokes on duplicate keys), and retry up to 3x
  on parse failure.
- **Golden-set tolerances need calibration**, especially for deliberately ambiguous cases
  (`case3_partial_fit` needed a wider range than the clear-cut strong/no-fit cases).
