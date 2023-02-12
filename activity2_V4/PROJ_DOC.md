# SER 321 Assignment 

## Running

gradle runClient -Pport=9999 -Phost='localhost' --console=plain -q 

gradle runServer -Pport=9999 

if the EC2 instance is running: 

gradle runClient -Pport=9099 -Phost=34.214.31.3 --console=plain -q 

## Requirements

### REQ1

The project needs to run through gradle.  See Running section above which describes how to
run the client and server.
### REQ2

The communication between the client and the server must follow the conventions specified in 
the [Protocol](PROTOCOL.md) document.

I don't have this tagged anywhere since the entire functionality of the server will determine
if this requirement is met. The server and the client each use a state machine to accomplish 
the interactions. Most of the communication on the server side is done in ClientHandler.java 
whereas the majority of the logic to handle responses is done in ClientIntf.java and
GameLogic.java.

### REQ3

The main menu gives the user 3 options, leaderboard, play game and quit, after a game is done the menu should pop up again. Client handles the menu. 

This is done by completing the menu structure that came with the provided code.

### REQ4

When the user chooses option 4, the leaderboard will be shown. This is done using the
Leaderboard.java module which will deal with reading from / writing to the nonvol leaderboard 

The leaderboard text is sent over to the client and it will display it whatever way 
it sees fit.
### REQ6 

The leaderboard presists. This was accomplished using JSON. See leaderboard.json and
Leaderboard.java for more details on this interaction.

### REQ7

The use selects option 2 and the current game board is shown. The ClientIntf.java module
handles the multiple client threads playing the game and is able to show the working
game board to new clients as they join the game. This happens immediately after a user
sends the NEW request operation type.

### REQ8 

Multiple users will be able to play the game. The server only runs a single instance of the
game. This is accomplished through the ClientInft module which controls multiple clients
playing the game. All of the game logic is nested within GameLogic.java and it simply sends
back a response to the ClientIntf after playing a move. It really has no concept of multiple
players so this is all controller on the above ClientIntf layer.

### REQ9

After winning a game the user gets 1 point. This is updated and tracked in the leaderboard
and updated occur immediately after game play.

### REQ10

After winning the client will go back to the main menu. This is accomplished through the
client's state machine. When a WIN signal is received, then the client knows that the game is
over and can show the menu again. There is no losing this game, only quitting.

### REQ11 

Multiple clients can win together and they will each get a point for winning. The ClientIntf
layer will track players that are currently in the game and then add a point to each player 
that is in the game when it ends. Sadly, when a client leaves the game, they do not get any 
points no matter the effort they put in.

### REQ12 

Tile coordinates are 2 character strings with the format \[letter\]\[number\].
This is handled in the game logic section where the above format gets converted
into a 1D coordinate into a game string.

### REQ13

The formatting of the game string is done in the GameLogic.java module as well

### REQ14 & REQ15

Client sends the first tile and if there are no errors, then it will be used in the
gameplay. Otherwise, if there is an error, then the server will send an error message.
Same goes for the second tile.

This is accomplished in the game module GameLogic.java which is responsible for sending
back the responses to the client.

### REQ16

The client presents the information so that a human can read it. While not perfect, I think
believe that this requirement is met.


### REQ17

After both tiles are shown, the user presses any key which will lead to the client
to just show the current board. This is done by a simple blocking call for standard input, 
I believe I used the enter key to accomplish this however instead of any key. For some reason
when I was using .read() and then scanning for input, it was not continuing, did not go back
to fix this so I left it as 'press enter key instead'

### REQ18

The game quits gracefully when option 3 is pressed. The client will shut down nicely.
The server, realizing the connection is terminated with the client, will close any
resources it has (although the OS probably does this anyway) and it will not service
this client any longer.

### REQ19 

The server does not crash when the client disconnects.See REQ18

### REQ20

TODO - if I forgot to update this doc, I am sure that I did this eventually

### REQ21

TODO - if I forgot to update this doc, I am sure that I did this eventually

### REQ22 (extra)

did not do this

### REQ23 

The game will exit if the player types 'exit' while playing the game. Done.

### REQ24

Server/client communication should be robust and should not crash.
Tested exceptions between clients and servers and have not found any
crashes yet.