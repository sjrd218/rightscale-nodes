#!/bin/sh -eu

if [ $# -ne 1 ]
then echo >&2 "usage: $0 href"; exit 2;
fi

if [ -f ~/.rsrc ]
then
  . ~/.rsrc
else
  echo >&2 "config file not found: ~/.rsrc. Create a file containing the following variables"
  echo >&2 "email="
  echo >&2 "pswd="
  echo >&2 "account="
  echo >&2 "url="
  exit 2
fi
 

RESOURCE=$1

curl -i -H X_API_VERSION:1.5 -c mycookie -X POST -d email="$email" -d password="$pswd" -d account_href=/api/accounts/"$account" ${url}/api/session

curl -i -H X_API_VERSION:1.5 -b mycookie -X GET ${url}${RESOURCE}.xml
echo
