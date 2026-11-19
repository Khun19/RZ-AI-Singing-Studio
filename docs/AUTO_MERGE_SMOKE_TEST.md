# Automatic Merge Smoke Test

This small documentation-only change is an end-to-end verification of the repository's automatic pull-request merge workflow.

The test is considered successful only if the PR is automatically queued by the workflow, all required `main` ruleset checks pass for the exact head commit, and GitHub merges it without a human approval or manual merge action.
