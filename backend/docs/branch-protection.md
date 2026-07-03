# Branch Protection & PR Workflow

This guide explains how to protect the `main` branch so that **all changes must go through a Pull Request** and require approval from the repository owner before merging.

---

## Workflow Overview

```
feature/my-change  →  Pull Request  →  Review (required)  →  Merge to main
```

No one — including the repository owner — can push directly to `main`. Every change must follow this flow:

1. Create a branch from `main`
2. Make changes on that branch
3. Open a Pull Request
4. Wait for required approval
5. Merge

---

## Step 1 — Set up CODEOWNERS

The `CODEOWNERS` file defines who must approve changes to any file in the repository.

Create the file at `.github/CODEOWNERS`:

```
# All files require approval from the repository owner
* @johnkbarrera
```

> Replace `@johnkbarrera` with your exact GitHub username (the one associated with `johnkevinbarrera@gmail.com`).

To find your GitHub username: go to [github.com](https://github.com) → click your profile picture → your username appears below your name.

---

## Step 2 — Configure Branch Protection on GitHub

1. Go to your repository on GitHub
2. Click **Settings** → **Branches**
3. Click **"Add branch ruleset"** *(not "Add classic branch protection rule")*

### Ruleset configuration

| Field | Value |
|-------|-------|
| **Ruleset name** | `protect-main` |
| **Enforcement status** | `Active` |

### Target branches

Click **"Add target"** → **Include by pattern** → type `main` → confirm.

### Branch rules — enable only this one

- [x] **Require a pull request before merging**
  - Required approvals: `1`
  - [x] Require review from Code Owners

Leave everything else **unchecked**, including **Restrict updates**.

> Do NOT enable **Restrict updates** — it blocks PR merges too, not just direct pushes. `Require a pull request` alone is enough to prevent direct pushes while still allowing PRs to be merged.

> **Require review from Code Owners** enforces `@johnkbarrera` as the mandatory approver on every PR — it reads from `.github/CODEOWNERS`.

### Bypass list — owner exception

Before saving, find the **Bypass list** section and add:

- Click **"Add bypass"** → search `@johnkbarrera` → select **Repository admin**

This allows `@johnkbarrera` to push directly to `main` when needed, while everyone else must go through a PR.

4. Click **"Create"**

---

## Step 3 — Verify the protection is active

Try pushing directly to `main`:

```bash
git checkout main
echo "test" >> README.md
git add README.md
git commit -m "test direct push"
git push origin main
```

You should see:

```
remote: error: GH006: Protected branch update failed for refs/heads/main.
remote: error: Required status check "..." is required.
To github.com:...
 ! [remote rejected] main -> main (protected branch hook declined)
```

---

## Daily Developer Workflow

Every change to the repository must follow these steps:

### 1. Create a branch

```bash
git checkout main
git pull origin main
git checkout -b feat/my-feature-name
```

Branch naming conventions:

| Prefix | Use |
|--------|-----|
| `feat/` | New feature |
| `fix/` | Bug fix |
| `chore/` | Maintenance, deps, config |
| `docs/` | Documentation only |

### 2. Make changes and push

```bash
# ... edit files ...
git add <files>
git commit -m "feat: describe what changed"
git push origin feat/my-feature-name
```

### 3. Open a Pull Request

```bash
gh pr create --title "feat: describe what changed" --base main
```

Or open it from GitHub: after pushing, click the **"Compare & pull request"** button that appears on the repository page.

### 4. Wait for approval

The PR will show:

```
Review required
At least 1 approving review is required by reviewers with write access.
```

The required reviewer (`johnkevinbarrera@gmail.com` / `@johnkbarrera`) will receive an email notification automatically.

### 5. Approve and merge (reviewer only)

The reviewer goes to the PR, reviews the changes, clicks **"Review changes"** → **"Approve"**, then clicks **"Merge pull request"**.

---

## Commit message convention

Use [Conventional Commits](https://www.conventionalcommits.org/) for consistency:

```
feat: add file upload endpoint
fix: resolve UUID validation error on confirm upload
chore: update dependencies
docs: add nginx setup guide
```

---

## Summary

| Action | Allowed |
|--------|---------|
| Push directly to `main` | No |
| Open a PR without approval | No (cannot merge) |
| Merge own PR without review | No |
| Approve and merge a PR | Yes — reviewer only (`@johnkbarrera`) |
| Create branches | Yes — anyone with access |
