blitz-admin - Fully automated controller for a Riichi-blitz tournaments
===================


Administrating tool for the most popular russian online riichi tournament series called Riichi-blitz.
It does all the stuff to provide smooth automated tournament inside Tenhou.net championship lobby.
It also integrates the Discord to invite the players into the voice rooms corresponding to their seatings.

The project contains of six main modules:
* **blitz** -- bloody pile'o'code that does a lot of work (controlling tournament timings, updating the website, generating seatings, notifying another modules, and many more)
* **blitzserver** -- heroku-compatible simple JSON API for running the admin on cloud with some parameters (i.e. lobby secret, particular game type)
* **client** -- Java interface for a tenhou.net protocol (mostly duplicates the one in tenso project) for a lobbybot
* **discord** -- helper for creating the Discord rooms and filling it with players during the tournament
* **lobbybot** -- a Tenhou bot who control the tournament state inside the actual lobby; it also grabs the results of the games
* **logger** -- helper for logging everything I need in a simpliest way without messing out with SLF4J

To deploy it on Heroku you just need to upload the sources on your instance. It will automatically build and install the project. See the "Procfile" and a "stage" task inside the "build.gradle" for details.

The project is published under the terms of the WTFPL.