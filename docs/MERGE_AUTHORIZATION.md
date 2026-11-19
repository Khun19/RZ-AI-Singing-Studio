# Automatic Pull Request Merge

## Goal

Merge eligible pull requests without a person manually approving or dispatching a merge. GitHub's native auto-merge is enabled automatically; GitHub remains responsible for enforcing the active repository ruleset and merging only when every required condition is satisfied.

## Automatic flow

1. A pull request targeting `main` is opened, reopened, updated, or marked ready for review.
2. The workflow ignores drafts and fork-originated pull requests. It only handles PRs whose head repository is this repository.
3. The workflow verifies that the PR is open, non-draft, targets `main`, belongs to this repository, and still has the exact head and base SHAs from the event.
4. It enables GitHub native auto-merge using squash and binds the request to the exact head SHA.
5. GitHub waits for all active ruleset requirements. When every requirement passes, GitHub merges automatically. No human approval or manual workflow dispatch is requested by this automation.

## Required repository safeguards

The active `Singing Music` ruleset remains authoritative. It currently requires:

- Strict required status checks: Gradle Wrapper bootstrap, JVM unit tests, Android emulator instrumentation tests, debug APK build, exact APK install/launch on API 31, and exact APK install/launch on API 35.
- Automatic Copilot code review requests on pushes (availability depends on the author's Copilot access and AI credits; this is not a human approval requirement).
- All review threads resolved.
- Linear history and squash-only merge.
- No force-push or branch deletion.

The ruleset currently sets required human approving reviews to zero. This is intentional: human approval is not a merge prerequisite. Copilot review is automatically requested by the ruleset, while required CI checks and review-thread resolution are actual merge gates. Do not disable or bypass these safeguards merely to make a PR merge.

## Fail-closed behavior

- Draft PRs and fork PRs are not queued by this workflow.
- A changed head/base SHA or mismatched PR identity stops the workflow.
- Failed, pending, missing, or stale required checks prevent GitHub from merging.
- Unresolved review threads or a failing Copilot review gate prevent merging.
- If GitHub auto-merge is disabled at repository level, the workflow fails rather than attempting a direct merge.
- The workflow never calls the immediate merge API and never bypasses the ruleset.

## Bootstrap behavior

A GitHub Actions workflow added in a pull request is not available to run for that same pull request until the workflow exists on the default branch. Therefore this PR is the bootstrap change. Once this PR is merged, the workflow automatically handles subsequent eligible PRs. For this bootstrap PR, native GitHub auto-merge must be queued once after the updated exact-head CI succeeds; GitHub will then apply the same ruleset gates without a human approval.

## Audit evidence

Each automatic queue attempt is recorded in GitHub Actions. The final merge event and merge commit are recorded by GitHub. Physical device acceptance remains a separate product release gate and is not represented by CI or PR merge status.
