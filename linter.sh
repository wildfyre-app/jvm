#!/bin/bash

# This file exists because of Arcanist's configuration.
# At the moment, this is the Arcanist behavior:
#  1 Linters are only called for unchanged files,
#  2 Linters are called once per file,
#  3 Linters are called concurrently.
# Because Gradle already handles all of this, they are incompatible.
# This script is an attempt to make them compatible.
# What it does is:
#  1 This script is called concurrently for every unchanged file.
#  2 Create a temporary file that holds the time in which the build
#    was executed for the last time.
#  3 If the file holds a time that was less than a few seconds ago, the
#    call to this script is aborted.
#  4 It seems counter-intuitive to write to the file *before* the build
#    rather than *after*, but this is needed because
#    of the concurrent calls. Otherwise, multiple builds would begin at
#    the same time (and compiling a project
#    concurrently is NOT a good idea - this is mostly because Arcanist
#    assumes static code analysis is done on the
#    source, but in the case of Gradle (and especially FindBugs), static
#    code analysis is performed on the resulting
#    bytecode).

# How many seconds happen between two different lintings
INVALIDATE_AFTER=15

# I am aware that this is bad and is a great loss of performance.
# This is the only way I found to wait until the Gradle daemon is ready.
# The use-case is that the script is called concurrently but tests are
# executed sequentially by the daemon, so since the timestamp is evaluated
# before the daemon call, the script still launches multiple times even
# with the timestamp. To keep that from happening, we call here a simple
# useless Gradle command, which will be executed by the daemon when it's
# ready. This ensures this run of the script awaits for the daemon to be
# clean until proceeding with the verification of the timestamp.
./gradlew --status > /dev/null

# Creation of the file if necessary
FILE=./.git/latest-build
[ ! -f "$FILE" ] && echo "1" > $FILE

# Reading of the latest date
# DATE=$(date --date=@$(cat $FILE))
DATE=$(cat $FILE)

# The limit is 10 seconds after the date found
let LIMIT=$DATE+$INVALIDATE_AFTER

# echo "Found date:   $DATE"
# echo "Limit:        $LIMIT"

# The current time, in milliseconds.
CURRENT=$(date +"%s")

# Writing the current date into the file
echo $CURRENT > $FILE

# echo "Current time: $CURRENT"

# During gradle build, when there's an error, this is what happens:
# The detail of the error is printed to stderr,
# A single line with the name of the project and of the task is printed to
# stdout, such has: > Task libwf-java:javadoc FAILED
# Arcanist expects the linters to work as in "a line is an error", so I
# ignore stderr (by forwarding it to /dev/null) and GREP the word 'FAILED'
# so that every line printed to stdout that does NOT contain the word is
# ignored (for example, the Gradle script prints the version number of the
# project). At the end, all is left as a result is the one-liners that
# only contain the name of the project & the task. This is enough to be
# sure that the linter will fail if there's an error and display the full
# name of the task that failed... if you need more information about the
# failure, you can re-run that single task yourself to get the details.
# Of course that's not as perfect as an actual linter written for Arcanist
# in PHP, but it's pretty good to the point that:
# If there's an error, Arcanist will fail and point to the specific task
# that failed (but it's up to you to rerun it to get the details)
# If there's no errors, Arcanist will not fail.
# That's not *perfect*, but that's reliable and is good enough that it
# won't cause any problems.
if [ $CURRENT -ge $LIMIT ]; then
    ./gradlew clean clean test check 2>/dev/null | grep --color=always FAILED
# else ignore the call because this script was already called in the last
# seconds
fi

# This script should always succeed: the linter succeeds even if there are
# problems in the build (reported in stdout).
exit 0
