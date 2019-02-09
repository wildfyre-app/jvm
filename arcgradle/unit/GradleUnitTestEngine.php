<?php

final class GradleUnitTestEngine extends ArcanistUnitTestEngine {

  protected function supportsRunAllTests() {
    return true;
  }


  public function shouldEchoTestResults() {
    return true;
  }

  public function run() {
    $startApiCommand = 'exec ./api.sh';
    $checkApiCommand = 'exec ./api.sh --norun';
    $gradleBaseCommand = './gradlew --console=plain -q --continue -PskipGetVersion ';
    $gradleTestCommand = $gradleBaseCommand . 'test machineReadableTestStatus';
    $gradleCheckCommand = $gradleBaseCommand . 'check machineReadableTestStatus';
    $startApiFuture = new ExecFuture($startApiCommand);
    $startApiFuture->setCWD($this->getWorkingCopy()->getProjectRoot());
    $startApiFuture->resolve(0);
    $err = 1;
    while ($err) {
      $checkApiFuture = new ExecFuture($checkApiCommand);
      $checkApiFuture->setCWD($this->getWorkingCopy()->getProjectRoot());
      list($err, $_stdout, $_stderr) = $checkApiFuture->resolve();
    }


    $testFuture = new ExecFuture($gradleTestCommand);
    $testFuture->setCWD($this->getWorkingCopy()->getProjectRoot());
    $startTime = microtime(true);
    list($_err, $testStdout, $testStderr) = $testFuture->resolve();
    $testTimeTaken = microtime(true) - $startTime;
    $testStdout = intval(trim($testStdout));


    $checkFuture = new ExecFuture($gradleCheckCommand);
    $checkFuture->setCWD($this->getWorkingCopy()->getProjectRoot());
    $startTime = microtime(true);
    list($_err, $checkStdout, $checkStderr) = $checkFuture->resolve();
    $checkTimeTaken = microtime(true) - $startTime;
    $checkStdout = intval(trim($checkStdout));

    list($apiStarted, $_stdout, $_stderr) = $startApiFuture->resolve(0);
    if ($apiStarted != 2) // We started the API, we must stop it also
      shell_exec('kill -2 $(' . $this->getWorkingCopy()->getProjectRoot() . '/api.sh --pid)');


    $ret = array();

    $tmp = id(new ArcanistUnitTestResult())
      -> setName('Unit Tests')
      -> setDuration($testTimeTaken)
//      -> setUserData(substr($testStderr, 0, strrpos(rtrim($testStderr), PHP_EOL)));
      -> setUserData(rtrim($testStderr));
    if (($testStdout & 0b01) == 0) {
      $ret[] = $tmp -> setResult(ArcanistUnitTestResult::RESULT_FAIL);
    } else {
      $ret[] = $tmp -> setResult(ArcanistUnitTestResult::RESULT_PASS);
    }

    $tmp = id(new ArcanistUnitTestResult())
      -> setName('SpotBugs')
      -> setDuration($checkTimeTaken)
      -> setUserData(rtrim($checkStderr));
    if (($checkStdout & 0b10) == 0) {
      $ret[] = $tmp -> setResult(ArcanistUnitTestResult::RESULT_FAIL);
    } else {
      $ret[] = $tmp -> setResult(ArcanistUnitTestResult::RESULT_PASS);
    }

    return $ret;
  }
}
