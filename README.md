# minesweeper

An unpolished Clojure Minesweeper clone with **an automatic solver**.

## Installation

Download the standalone jar from releases section (e.g. for 0.1.0):

`https://github.com/RokLenarcic/Minesweeper/releases/download/v0.1.0/minesweeper-0.1.0-standalone.jar`

### OR

Download source from:

`https://github.com/RokLenarcic/Minesweeper.git`

## Usage

Run standalone jar:

`java -jar minesweeper-0.1.0-standalone.jar`

### OR

Using Leiningen, from source:

    lein run

It's a standard minesweeper.

- `?` - unrevealed, left-click to expand, right-click to mark
- `!` - marked as mine, right-click to unmark
- `1-9` - number of adjacent mines, left-click will expand if the number is equal to the number of adjancent marked mines

Press `Ctrl-A` or `Cmd-A` (Mac) to run one iteration of the automatic solver, which will mark and reveal some fields.

The solver might choose to do nothing if there are no definite solutions (e.g. when playing field is completely empty).


### Missing pieces

- doesn't have unit tests.

## License

Copyright &copy; 2016 Rok Lenarcic

This project is licensed under the [GNU General Public License v3.0][license].

[license]: http://www.gnu.org/licenses/gpl-3.0.txt
