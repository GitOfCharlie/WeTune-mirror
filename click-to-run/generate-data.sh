#! /bin/bash

verbose=0
target=
optimizer="WeTune"
tag="base"

# read arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
  "-v" | "-verbose")
    verbose="${2}"
    shift 2
    ;;
  "-T" | "-target")
    target="${2}"
    shift 2
    ;;
  "-optimizer")
    optimizer="${2}"
    shift 2
    ;;
  "-tag")
    tag="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

if [ ! "$target" ]; then
  if [ "$tag" == 'base'] || [ "$tag" == 'zipf']; then
    target="used"
  else
    target="opt_used"
  fi
fi

echo "Begin generating data of ${tag} workload."
gradle :testbed:run \
    --args="GenerateTableData -v=${verbose} -target=${target} -optimizer=${optimizer} -tag=${tag}"
echo "Finish generating data of ${tag} workload."


echo "Begin importing data into database."

appName="all"
dbName=
appDataDir=
absoluteAppDataPath=
target_table=

host='localhost'
port='1433'
username='SA'
password='mssql2019Admin'

findAppDataDir() {
  local path="$1/$2"
  appDataDir=$(find "${data_dir}" -type d -wholename "*/$path" | head -1)
}

getAbsoluteDataPath() {
  local cwd=$(pwd)
  cd "${appDataDir}" || exit
  absoluteAppDataPath=$(pwd)
  cd "${cwd}" || exit
}

doTruncateOne() {
  local tableName=${1}

  echo "truncating ${tableName}"
#  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" -i "${data_dir}/schemas_mssql/${appName}.sql"
  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" <<EOF
    DELETE FROM ${tableName};
    GO
EOF
}

doImportOne() {
  local tableName=${1}
  getAbsoluteDataPath

  echo "importing ${tableName}"
  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" <<EOF
    ALTER TABLE [${tableName}] NOCHECK CONSTRAINT ALL;
    BULK INSERT [${tableName}] FROM '${absoluteAppDataPath}/${tableName}.csv' WITH( FIELDTERMINATOR=';', ROWTERMINATOR='\n' );
    ALTER TABLE [${tableName}] WITH CHECK CHECK CONSTRAINT ALL;
    GO
EOF
}

enableConstraints() {
  local tableName=${1}
  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" <<EOF
    ALTER TABLE [${tableName}] WITH CHECK CHECK CONSTRAINT ALL;
    GO
EOF
}

doImportData() {
  echo "gonna import $(find "$appDataDir" -maxdepth 1 -name '*.csv' | wc -l) target_tables in $appDataDir to $dbName@$host:$port"
  for fileName in "$appDataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    doTruncateOne "$tableName"
  done
  for fileName in "$appDataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    doImportOne "$tableName"
  done
  for fileName in "$appDataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    enableConstraints "$tableName"
  done
}


for app in 'broadleaf' 'diaspora' 'discourse' 'eladmin' 'fatfreecrm' 'febs' 'forest_blog' 'gitlab' 'guns' 'halo' 'homeland' 'lobsters' 'publiccms' 'pybbs' 'redmine' 'refinerycms' 'sagan' 'shopizer' 'solidus' 'spree'
do
  dbName=${app}_${tag}
  findAppDataDir "${tag}" "${app}"
  if [ ! "$appDataDir" ]; then
    continue
  fi

  doImportData
done
