## Description
This project is part of the Distributed Systems module in NUS. The requirement is to implement a distributed maze game with a simple game play design. The maze is an NxN grid, with treasures scattered randomly around in the cells. There can be two or more players in the game, and their objective is to move around and consume all the treasures on the maze. Each player is controlled by an instance of the app, and acts a peer. The peers can run locally or remotely within the network. Each player can move around the maze to consume treasures, one move at a time. Each peer can drop off (i.e. its process gets terminated at any time, with each termination having a reasonable lapse between each other).

The game is initiated with the first peer acting as the Primary server. Other peers connect to it using its IP address and port. The Primary server holds the game state, validates all players' moves and broadcasts updates to every other peers. To prevent the Primary server becoming the single point of failure, another peer is appointed as the Backup server. If the Primary server goes down, the Backup server becomes the new Primary, which is made known to all other peers, and one of the other peers will become the new Backup server. This requires that the game state is continuously communicated from the Primary to the Backup server in order to maintain it for the whole duration of the game.


## Implementation

![Screenshot](/screenshot.jpg?raw=true "Screenshot")

### Communication
We build the network communication of the project on Java RMI, as recommended by the instructor. It could have been built on any other RPC framework, but RMI is mature and simple enough to be selected for this project. Besides, the objective of the assignment is not about the framework as much as the high-level concepts for building a p2p distributed application.

## Game UI
We chose to implement the game UI based on JavaFX, because we were fascinated by the new power, productivity, and the freshness that JavaFX brings into the arsenal for building Desktop UI with Java.


## Thoughts on Implementation
As per requirements of the project, the game implementation is meant to be of a semi peer-to-peer nature. At any one time, there is a Primary server that maintains and updates the game state from the input by each player. The Primary server is prone to compromise and the game state can be manipulated by a malicious user. This vulnerability can be mitigated by having each peer verify the global game state using a consensus mechanism.


## Run the code

Compile the source code:
```
mvn compile
```

Launch a new instance:
```
mvn exec:java
```

Build an installable package using JavaFX's packaging tool:
```
mvn jfx:native
```


## Team Members
* Tony Luong (lpthanh{at}gmail.com)
* Chen Chi (chenchi2205{at}gmail.com)