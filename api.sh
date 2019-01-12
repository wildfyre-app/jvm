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
##################################################################

if [[ $(lsof -i :8000 | tail -1 | cut -d ' ' -f1 | grep python) ]]
then
    if [[ ! $1 = "--norun" ]]
    then
        echo "The API is running."
    fi

    exit 0 # Success
else
    if [[ $1 = "--norun" ]]
    then
        echo "Nothing is currently running on port 8000."
        echo "You might need to run ./api.sh to launch the API."

        exit 1 # Failure
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
        virtualenv env
    fi

    echo "Running the virtual environment..."
    source env/bin/activate

    echo "Installing requirements..."
    pip3.6 install -r requirements.txt

    echo "Preparing the database..."
    rm db.sqlite3
    python3.6 manage.py migrate

    echo "Starting the development server..."
    python3.6 manage.py sampledata
    python3.6 manage.py runserver
fi
