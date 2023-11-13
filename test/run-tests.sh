#!/bin/bash

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK_DIR="$(pwd)"
echo "WorkDir: $WORK_DIR"

#-----------------------------------#
# environments
#-----------------------------------#
JAVA_VER=${JAVA_VER:-8}
echo "JAVA_VER: $JAVA_VER"

FAIL_FAST=${FAIL_FAST:-0}
echo "FAIL_FAST: $FAIL_FAST"

SHOW_ERROR_DETAIL=${SHOW_ERROR_DETAIL:-0}
export SHOW_ERROR_DETAIL=$SHOW_ERROR_DETAIL
echo "SHOW_ERROR_DETAIL: $SHOW_ERROR_DETAIL"

maxForks=${FORK_COUNT:-2}
echo "FORK_COUNT: $maxForks"

#debug DEBUG=service1,service2
#deubg all duboo-xxx: DEBUG=dubbo*
export DEBUG=$DEBUG
echo "DEBUG=$DEBUG"

if [ "$MVN_OPTS" != "" ]; then
  export MVN_OPTS=$MVN_OPTS
  echo "MVN_OPTS: $MVN_OPTS"
fi

if [ "$BUILD_OPTS" == "" ]; then
  BUILD_OPTS="$MVN_OPTS --batch-mode --no-snapshot-updates --no-transfer-progress clean package dependency:copy-dependencies -DskipTests"
fi
export BUILD_OPTS=$BUILD_OPTS
echo "BUILD_OPTS: $BUILD_OPTS"

#-----------------------------------#
# constants
#-----------------------------------#
TEST_SUCCESS="TEST SUCCESS"
TEST_FAILURE="TEST FAILURE"
TEST_IGNORED="TEST IGNORED"

ERROR_MSG_FLAG=":ErrorMsg:"

CONFIG_FILE="case-configuration.yml"

# Exit codes
# ignore testing
EXIT_IGNORED=120

#-----------------------------------#
# functions
#-----------------------------------#
abspath() { case "$1" in /*) printf "%s\n" "$1" ;; *) printf "%s\n" "$PWD/$1" ;; esac }

function get_error_msg() {
  log_file=$1
  error_msg=$(grep $ERROR_MSG_FLAG $log_file)
  error_msg=${error_msg#*$ERROR_MSG_FLAG}
  echo $error_msg
}

function print_log_file() {
  scenario_name=$1
  file=$2

  if [ -f $file ]; then
    title="$scenario_name log: $(basename $file)"
    echo ""
    echo "----------------------------------------------------------"
    echo " $title"
    echo "----------------------------------------------------------"
    cat $file
    echo ""
  fi
}

function check_test_image() {
  #check dubbo/sample-test image and version
  test_image="dubbo/sample-test:$JAVA_VER"
  echo "Checking test image [$test_image] .. "
  docker images --format 'table {{.Repository}}:{{.Tag}}\t{{.ID}}\t{{.CreatedAt}}\t{{.Size}}' | grep $test_image
  result=$?
  if [ $result != 0 ]; then
    echo "Test image not found: $test_image, please run 'bash ./build-test-image.sh' first."
    exit 1
  fi
}

function process_case() {
  case_dir=$1
  case_no=$2

  if [ -f $case_dir ]; then
    case_dir=$(dirname $case_dir)
  fi

  file=$case_dir/$CONFIG_FILE
  if [ ! -f $file ]; then
    echo "$TEST_FAILURE: case config not found: $file" | tee -a $testResultFile
    return 1
  fi

  case_start_time=$SECONDS
  project_home=$(dirname $file)
  scenario_home=$project_home/target
  scenario_name=$(basename $project_home)
  log_prefix="[${case_no}/${totalCount}] [$scenario_name]"
  echo "$log_prefix Processing : $project_home .."

  start_time=$SECONDS
  log_prefix="[${case_no}/${totalCount}] [$scenario_name]"

  # run test using version profile
  echo "$log_prefix Building project : $scenario_name .."
  cd $project_home

  # clean target manual, avoid 'mvn clean' failed with 'Permission denied' in github actions
  find . -name target -d | xargs -I {} rm -rf {}
  target_dirs=$(find . -name target -d)
  if [ "$target_dirs" != "" ]; then
    echo "$log_prefix Force delete target dirs"
    find . -name target -d | xargs -I {} sudo rm -rf {}
  fi

  mvn $BUILD_OPTS &>$project_home/mvn.log
  result=$?
  if [ $result -ne 0 ]; then
    echo "$log_prefix $TEST_FAILURE: Build failure , please check log: $project_home/mvn.log" | tee -a $testResultFile
    if [ "$SHOW_ERROR_DETAIL" == "1" ]; then
      cat $project_home/mvn.log
    fi
    return 1
  fi

  # generate case configuration
  mkdir -p $scenario_home/logs
  scenario_builder_log=$scenario_home/logs/scenario-builder.log
  echo "$log_prefix Generating test case configuration .."
  config_time=$SECONDS
  java -Dconfigure.file=$file \
    -Dscenario.home=$scenario_home \
    -Dscenario.name=$scenario_name \
    -Dscenario.version=$version \
    -Dtest.image.version=$JAVA_VER \
    -Ddebug.service=$DEBUG \
    -jar $test_builder_jar &>$scenario_builder_log
  result=$?
  if [ $result -ne 0 ]; then
    error_msg=$(get_error_msg $scenario_builder_log)
    if [ $result == $EXIT_IGNORED ]; then
      echo "$log_prefix $TEST_IGNORED: $error_msg" | tee -a $testResultFile
    else
      echo "$log_prefix $TEST_FAILURE: Generate case configuration failure: $error_msg, please check log: $scenario_builder_log" | tee -a $testResultFile
    fi
    return $result
  fi

  # run test
  echo "$log_prefix Running test case .."
  running_time=$SECONDS
  bash $scenario_home/scenario.sh
  result=$?
  end_time=$SECONDS

  if [ $result == 0 ]; then
    echo "$log_prefix $TEST_SUCCESS , cost $((end_time - start_time)) s"
  else
    scenario_log=$scenario_home/logs/scenario.log
    error_msg=$(get_error_msg $scenario_log)
    if [ $result == $EXIT_IGNORED ]; then
      if [ "$error_msg" != "" ]; then
        echo "$log_prefix $TEST_IGNORED: $error_msg, " | tee -a $testResultFile
      else
        echo "$log_prefix $TEST_IGNORED, please check logs: $scenario_home/logs" | tee -a $testResultFile
      fi
    else
      if [ "$error_msg" != "" ]; then
        echo "$log_prefix $TEST_FAILURE: $error_msg, please check logs: $scenario_home/logs" | tee -a $testResultFile
      else
        echo "$log_prefix $TEST_FAILURE, please check logs: $scenario_home/logs" | tee -a $testResultFile
      fi
    fi

    # show test log
    if [ "$SHOW_ERROR_DETAIL" == "1" ]; then
      for log_file in $scenario_home/logs/*.log; do
        # ignore scenario-builder.log
        if [[ $log_file != *scenario-builder.log ]]; then
          print_log_file $scenario_name $log_file
        fi
      done
    fi
    return 1
  fi

  log_prefix="[${case_no}/${totalCount}] [$scenario_name]"
  echo "$log_prefix $TEST_SUCCESS, total cost $((end_time - case_start_time)) s" | tee -a $testResultFile

  # clean log files
  rm -f $project_home/*.log
}

#-----------------------------------#
# main
#-----------------------------------#
#TEST_CASE_FILE
if [ "$TEST_CASE_FILE" != "" ]; then
  # convert relative path to absolute path
  if [[ $TEST_CASE_FILE != /* ]]; then
    TEST_CASE_FILE=$(abspath $TEST_CASE_FILE)
  fi
  echo "TEST_CASE_FILE: $TEST_CASE_FILE"
fi

echo "Test logs dir: \${project.basedir}/target/logs"
echo "Test reports dir: \${project.basedir}/target/test-reports"

# prepare testcases
mkdir -p $TEST_DIR/jobs
testListFile=$TEST_DIR/jobs/testjob.txt
targetTestcases=$1
if [ "$targetTestcases" != "" ]; then
  targetTestcases=$(abspath $targetTestcases)
  if [ -d "$targetTestcases" ] || [ -f "$targetTestcases" ]; then
    echo "Target testcase: $targetTestcases"
    echo $targetTestcases >$testListFile
  else
    echo "Testcase not exist: $targetTestcases"
    exit 1
  fi
else
  # use input testcases file
  if [ "$TEST_CASE_FILE" != "" ]; then
    testListFile=$TEST_CASE_FILE
    if [ ! -f $testListFile ]; then
      echo "Testcases file not found: $testListFile"
      exit 1
    fi
  else
    # find all case-configuration.yml
    base_dir="$(dirname $TEST_DIR)"/test/scenarios
    rm -f $testListFile
    echo "Searching all '$CONFIG_FILE' under dir $base_dir .."
    find $base_dir -name $CONFIG_FILE >$testListFile
  fi
fi

totalCount=$(grep "" -c $testListFile)
echo "Total test cases : $totalCount"

if [ "$DEBUG" != "" ] && [ $totalCount -gt 1 ]; then
  echo "Only one case can be debugged"
  exit 1
fi

#clear test results
testResultFile=${testListFile%.*}-result-java${JAVA_VER}.txt
rm -f $testResultFile
touch $testResultFile
echo "Test results: $testResultFile"

# build scenario-builder
SCENARIO_BUILDER_DIR=$TEST_DIR/dubbo-scenario-builder
echo "Building scenario builder .."
cd $SCENARIO_BUILDER_DIR
mvn $BUILD_OPTS &>$SCENARIO_BUILDER_DIR/mvn.log
result=$?
cd $WORK_DIR
if [ $result -ne 0 ]; then
  echo "Build dubbo-scenario-builder failure, please check logs: $SCENARIO_BUILDER_DIR/mvn.log"
  cat $SCENARIO_BUILDER_DIR/mvn.log
  exit $result
fi

# find jar
test_builder_jar=$(ls $SCENARIO_BUILDER_DIR/target/dubbo-scenario-builder*-with-dependencies.jar)
if [ "$test_builder_jar" == "" ]; then
  echo "dubbo-scenario-builder jar not found"
  exit 1
else
  echo "Found test builder : $test_builder_jar"
fi

#check test image
check_test_image

# start run tests
testStartTime=$SECONDS

#counter
allTest=0
finishedTest=0

while read path; do
  allTest=$((allTest + 1))

  if [ -f $path ]; then
    path=$(dirname $path)
  fi
  # fork process testcase
  process_case $path $allTest &
  sleep 1

  #wait for tests finished
  delta=$maxForks
  if [ $allTest == $totalCount ]; then
    delta=0
  fi
  while [ $finishedTest -lt $totalCount ] && [ $((allTest - finishedTest)) -ge $delta ]; do
    sleep 1
    if [ -f $testResultFile ]; then
      finishedTest=$(grep "" -c $testResultFile)
      if [ "$finishedTest" == "" ]; then
        finishedTest=0
        continue
      fi
      # check fail fast
      if [ "$FAIL_FAST" == "1" ]; then
        failedTest=$(grep "$TEST_FAILURE" -c $testResultFile)
        if [ $failedTest -ne 0 ]; then
          echo "Aborting, wait for subprocess finished .."
          wait
          echo "----------------------------------------------------------"
          echo "Test is aborted cause some testcase is failed (fail-fast mode). "
          echo "Fail tests:"
          grep "$TEST_FAILURE" $testResultFile
          echo "----------------------------------------------------------"
          exit 1
        fi
      fi
    fi
  done

done <$testListFile

successTest=$(grep "$TEST_SUCCESS" -c $testResultFile)
failedTest=$(grep "$TEST_FAILURE" -c $testResultFile)
ignoredTest=$(grep "$TEST_IGNORED" -c $testResultFile)

echo "----------------------------------------------------------"
echo "Test logs dir: \${project.basedir}/target/logs"
echo "Test reports dir: \${project.basedir}/target/test-reports"
echo "Test results: $testResultFile"
echo "Total cost: $((SECONDS - testStartTime)) seconds"
echo "All tests count: $totalCount"
echo "Success tests count: $successTest"
echo "Ignored tests count: $ignoredTest"
echo "Failed tests count: $failedTest"
echo "----------------------------------------------------------"

if [ $ignoredTest -gt 0 ]; then
  echo "Ignored tests: $ignoredTest"
  grep "$TEST_IGNORED" $testResultFile
  echo "----------------------------------------------------------"
fi

if [ $failedTest -gt 0 ]; then
  echo "Failed tests: $failedTest"
  grep "$TEST_FAILURE" $testResultFile
  echo "----------------------------------------------------------"
fi

echo "Total: $totalCount, Success: $successTest, Failures: $failedTest, Ignored: $ignoredTest"

if [[ $successTest -gt 0 && $(($successTest + $ignoredTest)) == $totalCount ]]; then
  test_result=0
  echo "All tests pass"
else
  test_result=1
  if [[ $failedTest -gt 0 ]]; then
    echo "Some tests failed: $failedTest"
  elif [ $successTest -eq 0 ]; then
    echo "No test pass"
  else
    echo "Test not completed"
  fi
fi
exit $test_result
