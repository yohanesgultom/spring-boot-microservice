#!/bin/bash
buckets_all="$COUCHBASE_BUCKET_NAME"

# concat additional buckets if defined
if [ -n "$COUCHBASE_ADDITIONAL_BUCKETS" ]; then  
  buckets_all="$buckets_all,$COUCHBASE_ADDITIONAL_BUCKETS"
fi

# loop until all buckets created
cmd="curl -s -u $COUCHBASE_USERNAME:$COUCHBASE_PASSWORD http://couchbase:8091/pools/default/buckets"
buckets_info=$(eval "$cmd")
max_count=300
count=0
for b in $(echo $buckets_all | sed "s/,/ /g")
do
  printf "Waiting for bucket $b"
  until [[ $buckets_info == *"buckets/$b?bucket_uuid"* ]]; do
    if [[ $count -gt $max_count ]]; then
      echo $buckets_info
      echo "$(tput setaf 1)Timeout error$(tput sgr0)"
      exit 1
    fi
    ((count++))    
    buckets_info=$(eval "$cmd")
    # echo $buckets_info
    >&2 printf "."        
    sleep 1
  done
  echo ""
done
echo "$(tput setaf 2)Couchbase is ready$(tput sgr0)"