# Refactoring Opportunities — Lab 4

## Selection criteria

Part 2, A.2 of the assignment requires a table with four columns: location, code smell name, refactoring name and student number. Section A.3 requires distributing the opportunities so that each member applies **between 5 and 10 refactorings of different types** in their branch. Different students may use the same refactoring type at different locations.

There is no requirement to list every Qodana finding. This table presents a **selection of 15 opportunities: five different types per student**. The selection meets the required minimum; further justified opportunities may be added within the individual limit. POM dependency findings are outside this selection of Java code refactorings.

All selected locations belong to the `battleship` package. The corresponding tests have already been implemented in other branches, as confirmed by the group. This is a **proposed allocation for the group to review**, based on the current source code; it does not represent completed refactorings or confirmed findings from a new Qodana run.

## Opportunities table

| Location (class or class::method) | Code smell name | Refactoring name | Student number |
|---|---|---|---|
| `Tasks::runMenu` — the `GERAFROTA` and `LEFROTA` command blocks, which reset the game, start the clock and update the boards | **Duplicated Code / Long Method** — common new-game setup is repeated within a method that handles every command | **Extract Method** — extract the shared setup, keeping random fleet creation and manual fleet input in their respective command handlers | 113146 |
| `Leaderboard::printTop5` — literal `5` in `Math.min(5, results.size())` | **Magic Number** — the maximum leaderboard size is a literal embedded in the algorithm | **Extract Constant** — introduce `TOP_RESULTS_LIMIT`, preserving the current value and behavior | 113146 |
| `Tasks::buildFleet` — local variables `i` and `s` | **Mysterious Name** — the names do not explain the counter of successfully added ships or the ship read from input | **Rename** — use `shipsAdded` and `ship`, updating references within the method | 113146 |
| `GameClock` — the `moveTimesMs` field, recording in `endMove`, and average/minimum/maximum calculations in `summary` | **Divergent Change** — the same class manages stopwatch lifecycles and volley statistics | **Extract Class** — extract the history and calculations into `MoveStatistics`, retaining the stopwatches and the `GameClock` API | 113146 |
| `Tasks::readShip` — variable `c`, used only to initialize `bearing` | **Redundant Temporary Variable** — the intermediate variable has no descriptive name and is used only once | **Inline Variable** — inline `in.next().charAt(0)` into the call to `Compass.charToCompass`, preserving scanner input order | 113146 |
| `Game::randomEnemyFire` — building usable positions and excluding previous shots and cells adjacent to sunk ships | **Long Method** — candidate preparation is mixed with random selection, firing and presentation | **Extract Method** — extract candidate construction into a method with an explicit responsibility | 129360 |
| `Game::jsonShots` — position conversion and JSON serialization | **Divergent Change** — the game model also knows the transport format and Jackson implementation details | **Extract Class** — extract conversion/serialization into `ShotJsonSerializer`, keeping `Game.jsonShots` as a compatible delegation and preserving output, exception messages and causes | 129360 |
| `Ship::getTopMostPos` — expression `getPositions().get(i).getRow()`, repeated in the comparison and assignment | **Duplicate Expression** — the current position's row is retrieved twice in the same search step | **Introduce Variable** — introduce `currentRow` inside the loop and use it in both expressions | 129360 |
| `Game::printBoard` — parameter `show_shots` | **Inconsistent Naming** — the parameter uses snake_case while the other parameters follow Java naming conventions | **Rename** — rename the parameter to `showShots`, without changing the logic or parameter types | 129360 |
| `Game::printBoard`, `Game::printMyBoard` and `Game::printAlienBoard` — the pair of boolean options controlling shots and the legend | **Data Clumps / Flag Arguments** — the same display options travel together, and calls with two booleans are unclear | **Introduce Parameter Object** — group the options into `BoardDisplayOptions`; retain compatible overloads/delegations for existing public methods | 129360 |
| `Move::processEnemyFire` — the `if (verbose)` block that builds and prints the text summary | **Long Method / Divergent Change** — result counting, text presentation and JSON serialization coexist in one method | **Extract Method** — extract summary formatting/presentation, preserving output order and text and the JSON results | 129763 |
| `Fleet::colisionRisk` | **Mysterious Name / Inconsistent Naming** — the name contains a spelling error and does not clearly express a predicate | **Rename** — rename the private method to `hasCollisionRisk` and update its call in `addShip` | 129763 |
| Constructors of `Caravel`, `Carrack` and `Frigate` — loops that build a linear ship's positions according to its bearing | **Duplicated Code** — the three subclasses repeat the same logic, differing only in category and size | **Pull Up Method** — extract linear position construction and move the shared method into `Ship`; preserve the exact current coordinates for every bearing | 129763 |
| `Position::adjacentPositions` — local `directions` array containing eight offsets | **Magic Numbers / Repeated Data Initialization** — the same fixed offsets are defined again on every call | **Extract Constant** — extract the array into a private `ADJACENT_OFFSETS` constant, without exposing it or changing neighbor values or order | 129763 |
| `Fleet::addShip` — expression `(ships.size() <= FLEET_SIZE) && s.isInsideBoard() && !colisionRisk(s)` | **Complicated Conditional** — three acceptance conditions are embedded in the `if`, without a name expressing the decision | **Introduce Variable** — introduce `canAddShip` for the complete expression, preserving short-circuit evaluation and the current limit | 129763 |

## Task allocation

- **113146 — Ricardo Oliveira:** Extract Method, Extract Constant, Rename, Extract Class and Inline Variable.
- **129360 — Francisco Farinha:** Extract Method, Extract Class, Introduce Variable, Rename and Introduce Parameter Object.
- **129763 — Rebeca Noronha:** Extract Method, Rename, Pull Up Method, Extract Constant and Introduce Variable.
