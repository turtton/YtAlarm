 

# Contributing guide(English/[日本語](../docs/contributing/CONTRIBUTING_ja.md))

Thank you for reading. If you have any question about this, please create new [discussion](https://github.com/turtton/YtAlarm/discussions/categories/q-a)

## Bug report / Feature request

- Check to see if a similar issue has already been reported before creating a new one.
- Follow the template when reporting (if ignored, it will close without looking at the content).
- Content can be written in English or Japanese.

Describe only one bug or feature per issue.

## Translation

### Documents

Both new creations and suggestions for modification of existing files are welcome.  
Below is a guide for creating a new one.

- Create specify files in [docs/readme](../docs/readme) or [docs/contributing](../docs/contributing)
- Each file should be named with the name of the target, [the language code](https://www.loc.gov/standards/iso639-2/php/code_list.php) and if nesecessary, [the region code](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en).(e.g. `README_en_uk.md`)
- Once finished, create a PR with a title starting with `docs:`.
- Not allowed to push code directly to the main or other major branches without a specific reason.

### Application

Coming soon.

## Development

### Guidelines

- Java code is not accepted unless there is a particular reason.
- Coding should be written following the [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide?hl=en), but please follow `ktlint` and `detekt` for the final version.
- Pay close attention to the license when adding libraries.(make sure it does not include closed-source library)

### Starting development

- If the issue you want to help out with exists, leave a comment on it saying you want to try a hand at it.
  If the appropriate issue does not exist, please create new one.
- Minimize the number of Issues to be resolved in a single PR.

#### Branch name

Please give a brief description of the change or issue number. (e.g., `fix/88`,`feat/display_progress`)

#### Commit message

Recommend naming it according to [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

### Pull requests

- The title should be named according to [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).
- Follow the template (if ignored, it will close without looking at the content).
- It is possible to create a PR event if you are in the middle of a work in progress to help us keep track of your progress.
  In that case, please set it as a draft.

### IDE

This application is developed using [Android Studio](https://developer.android.com/studio). Please make sure that the code style is overridden by [`.editorconfig`](../.editorconfig) when you load the project.

It is free to develop in other editors, but please make sure that [`.editorconfig`](../.editorconfig) is supported.
Also, please do not commit any workspace configuration files such as `.fleet` .

### Code Checking

Uses `ktlint`, `detekt` and `Android Plugin for Gradle`. It can be run from the following Gradle task.

- Format: `formatKotlin`
- Lint: `lintKotlin detekt lint`
- Check all: `check`



