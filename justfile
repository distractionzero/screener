gradle_file := "app/app/build.gradle.kts"
changelog_dir := "fastlane/metadata/android/en-US/changelogs"
apk_dir := "app/app/build/outputs/apk/release"

release bump:
    #!/usr/bin/env bash
    set -euo pipefail

    if [[ -n "$(git status --porcelain)" ]]; then
        echo "working tree is not clean" >&2
        exit 1
    fi
    if ! command -v gh > /dev/null; then
        echo "gh is not installed" >&2
        exit 1
    fi

    code=$(sed -nE 's/^\s*versionCode = ([0-9]+)$/\1/p' {{gradle_file}})
    name=$(sed -nE 's/^\s*versionName = "([0-9]+\.[0-9]+\.[0-9]+)"$/\1/p' {{gradle_file}})
    if [[ -z "$code" || -z "$name" ]]; then
        echo "could not read versionCode/versionName from {{gradle_file}}" >&2
        exit 1
    fi

    IFS=. read -r major minor patch <<< "$name"
    case "{{bump}}" in
        major) major=$((major + 1)); minor=0; patch=0 ;;
        minor) minor=$((minor + 1)); patch=0 ;;
        patch) patch=$((patch + 1)) ;;
        *) echo "usage: just release {patch,minor,major}" >&2; exit 1 ;;
    esac
    new_name="$major.$minor.$patch"
    new_code=$((code + 1))

    if git rev-parse -q --verify "refs/tags/v$new_name" > /dev/null; then
        echo "tag v$new_name already exists" >&2
        exit 1
    fi

    last_tag=$(git describe --tags --abbrev=0 2>/dev/null || true)
    range=${last_tag:+$last_tag..HEAD}
    commits=$(git log --no-merges --format='- %s' $range \
        | grep -vE '^- (chore|docs|ci|style|test|build|refactor)(\(.*\))?!?:' || true)

    # F-Droid truncates changelogs over 500 chars, so keep whole lines that fit
    notes=""
    while IFS= read -r line; do
        [[ -z "$line" ]] && continue
        candidate="${notes:+$notes$'\n'}$line"
        (( ${#candidate} > 500 )) && break
        notes="$candidate"
    done <<< "$commits"
    [[ -n "$notes" ]] || notes="- Minor improvements"

    sed -i -E "s/(versionCode = )[0-9]+/\1$new_code/" {{gradle_file}}
    sed -i -E "s/(versionName = \")[0-9]+\.[0-9]+\.[0-9]+\"/\1$new_name\"/" {{gradle_file}}
    mkdir -p {{changelog_dir}}
    printf '%s\n' "$notes" > "{{changelog_dir}}/$new_code.txt"

    (cd app && ./gradlew clean assembleRelease)
    if [[ ! -f {{apk_dir}}/app-release.apk ]]; then
        echo "no signed apk in {{apk_dir}}, check the SCREENER_* signing properties" >&2
        exit 1
    fi
    apk="{{apk_dir}}/screener-$new_name.apk"
    cp {{apk_dir}}/app-release.apk "$apk"

    git add {{gradle_file}} "{{changelog_dir}}/$new_code.txt"
    git commit -m "chore: release $new_name"
    git tag -a "v$new_name" -m "v$new_name" -m "$notes"

    echo "released v$new_name ($new_code)"
    printf '%s\n' "$notes"
    git push --follow-tags
    gh release create "v$new_name" "$apk" --verify-tag --title "v$new_name" --notes "$notes"

    commit=$(git rev-parse "v$new_name^{commit}")
    echo
    echo "metadata/dev.codingcoffee.screener.yml for fdroiddata:"
    echo
    cat <<EOF
    Categories:
      - Phone & SMS
    License: GPL-3.0-only
    AuthorName: Ameya Shenoy
    WebSite: https://screener.codingcoffee.dev
    SourceCode: https://github.com/distractionzero/screener
    IssueTracker: https://github.com/distractionzero/screener/issues
    Changelog: https://github.com/distractionzero/screener/tags

    AutoName: Screener

    RepoType: git
    Repo: https://github.com/distractionzero/screener.git

    Builds:
      - versionName: $new_name
        versionCode: $new_code
        commit: $commit
        subdir: app/app
        gradle:
          - yes

    AutoUpdateMode: Version
    UpdateCheckMode: Tags
    CurrentVersion: $new_name
    CurrentVersionCode: $new_code
    EOF
