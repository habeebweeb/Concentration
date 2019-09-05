# Concentration
A game of concentration that can be played over the internet. Originally written in spring 2017.
# Usage
Compile with `javac`. There are 2 programs `GameServer` and `Player`
## GameServer
```
java GameServer [-clients <number of clients>] [-width <width>] [-height <height>] [-help]

 -clients  number of clients per game
 -width    width of game board
 -height   height of game board
 -help     show this help
```
the `width` and `height` are restricted to an integer range from 2 - 6 and both will default to 2 if `width * height` ends up being an odd number
## Player
```
java Player -server <server address> [-img <images directory>] [-help]

 -server  address of the server
 -img     directory where images are stored
 -help    show this help
```
`img` points to a directory where there are a total 20 images in the `jpg` format and include 1 image named `back.jpg` a sequence of 19 images with the names `n.jpg` from 0 to 19
