#!/usr/bin/env bash

set -euo pipefail

COUNT=$(git rev-list --count HEAD)
HASH=$(git rev-parse --short=10 HEAD)

TAG="release/1.11-$COUNT-$HASH"
mvn -U clean package
mkdir -p release/
REL="sidekt-1.11-$COUNT-$HASH.jar"
cp "target/sidekt-1.11.0.jar" "release/$REL"
md5sum "release/$REL" > "release/$REL.md5"
sha256sum "release/$REL" > "release/$REL.sha256"

# tag
git tag "$TAG"
echo "tagged release: $TAG"
