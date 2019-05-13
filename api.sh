#!/usr/bin/env bash

##################################################################
#                                                                #
#       This script is not Windows-compatible.                   #
#       Windows users, you have to ensure the API                #
#       is running by yourself.                                  #
#                                                                #
#       Usage:                                                   #
#                                                                #
# ./api.sh                                                       #
#   Launches the API if it's not already running                 #
#                                                                #
# ./api.sh --norun                                               #
#   Displays an error message if the API is not running,         #
#   or exits silently.                                           #
#                                                                #
# ./api.sh --pid                                                 #
#   Displays the PID of what is running on port 8000             #
#                                                                #
##################################################################

if [[ $1 = "--pid" ]]
then
    echo $(lsof -i :8000 -sTCP:LISTEN | tail -1 | cut -d ' ' -f2)
    exit 0
fi

if [[ $(lsof -i :8000 -sTCP:LISTEN | tail -1 | cut -d ' ' -f2) ]]
then
    if [[ ! $1 = "--norun" ]]
    then
        echo "Something is running on port 8000."
        exit 2
    fi

    exit 0 # --norun exits successfully if its running already
else
    if [[ $1 = "--norun" ]]
    then
        if which lsof >/dev/null
        then
            echo "Nothing is currently running on port 8000."
            echo "You might need to run ./api.sh to launch the API."

            exit 1 # Failure
        else
            echo "Command 'lsof' is not installed, impossible to know if the API is running or not."

            exit 1
        fi
    fi

    echo "Setting up the submodule(s)..."
    git submodule init
    git submodule update

    echo "Checking if there is a new version of the API..."
    cd api
    git fetch origin master # Check if there are new commits, but don't merge them
    git --no-pager log --decorate=short ..FETCH_HEAD  # Display newer commits, if any

    cd api
    if [[ ! -d "env" ]]
    then
        echo "The virtual environment doesn't exist, creating it..."
        python3 -m venv env/
    fi

    echo "Running the virtual environment..."
    source env/bin/activate

    echo "Installing requirements..."
    python3 -m pip install -r requirements.txt

    echo "Preparing the database..."
    test -f db.sqlite3 && rm db.sqlite3
    python3 manage.py migrate

    echo "Starting the development server..."
    python3 manage.py sampledata
    python3 manage.py runserver &
fi
