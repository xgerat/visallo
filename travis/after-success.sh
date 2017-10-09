#!/bin/bash -e

if [ "${BUILD_DOCS}" ]; then

  if [ -d "docs/_book" ]; then
    docker run --volume $(pwd):/root/visallo \
         -w /root/visallo \
         -e "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}" \
         -e "AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}" \
         -e "TRAVIS_BRANCH=${TRAVIS_BRANCH}" \
         -e "VERSION_ROOT=${VERSION_ROOT}" \
         --rm -it visallo/travis:visallo-4.0.0 \
         /bin/sh -c "travis/publish-docs.sh"
  fi
else
  if [ ${TRAVIS_REPO_SLUG} = "visallo/visallo" ]; then
    if [ ${TRAVIS_BRANCH} = "master" ] || echo ${TRAVIS_BRANCH} | grep -Eq '^release-.*$'; then
      if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
        docker run --volume ${HOME}/.m2/repository:/root/.m2/repository \
             --volume ${HOME}/.npm:/root/.npm \
             --volume $(pwd):/root/visallo \
             -w /root/visallo \
             -e "MVN_REPO_USERNAME=${DEPLOY_USERNAME}" \
             -e "MVN_REPO_PASSWORD=${DEPLOY_PASSWORD}" \
             --rm -it visallo/travis:visallo-4.0.0 \
             /bin/sh -c "mvn -B -f root/pom.xml deploy && mvn -B -DskipTests deploy"
      fi
    fi
  fi

  CREATED_BY="${TRAVIS_REPO_SLUG} commit "`git rev-parse --short HEAD`""
  if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then ./travis/travis-request.sh --repo ${TRAVIS_DOWNSTREAM_REPO} --token ${TRAVIS_ACCESS_TOKEN_PRO} --by "${CREATED_BY}" --branch ${TRAVIS_BRANCH}; fi
fi
