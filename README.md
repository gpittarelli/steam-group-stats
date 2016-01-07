# Steam Group Stats Grabber

A simple script for quickly grabbing simple activity statistics of a
steam group, run as:

    # Fetches stats from http://steamcommunity.com/groups/TF2StadiumAlpha
    ./stats TF2StadiumAlpha

Requires Python 3 and the python libraries listed in
`requirements.txt`. These can be installed with:

    pip install -r requirements.txt

Python 2 may work, but has not been tested, and will require running
the script explicitly with `python2 scan` or by modifying the first
line shebang in `scan` to use `python2` instead of `python3`.
