
protoc.exe --java_out=./  ./proto/game.proto
protoc.exe --java_out=./  ./proto/gate.proto

XCOPY .\msg\Game.java ..\..\..\..\gate\src\main\java\msg\*.* /S /Y
XCOPY .\msg\Gate.java ..\..\..\..\gate\src\main\java\msg\*.* /S /Y


pause