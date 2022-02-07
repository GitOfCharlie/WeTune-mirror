#! /bin/bash

data_dir="${WETUNE_DATA_DIR:-wtune_data}"
verbose='0'
rewrite_dir="rewrite"
rules='rules/rules.txt'

while [[ $# -gt 0 ]]; do
  case "$1" in
  "-R" | "-rules")
    rules="${2}"
    shift 2
    ;;
  "-v" | "-verbose")
    verbose="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

set -- "${positional_args[@]}"

gradle :superopt:run --args="RewriteQuery -v=${verbose} -R=${rules}"

cd "${data_dir}/${rewrite_dir}" || exit

dir=$(ls -t -1 | ag 'run.+' | head -1)
ln -sfr "${dir}" 'result'
echo "$(cut -f1 'result/1_query.txt' | uniq | wc -l) queries rewritten."