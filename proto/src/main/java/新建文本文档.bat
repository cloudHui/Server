
protoc.exe --java_out=./  ./game.proto
protoc.exe --java_out=./  ./gate.proto
protoc.exe --java_out=./  ./hall.proto

::可以不用复制
::XCOPY .\proto\GateProto.java ..\..\..\..\gate\src\main\java\msg\*.* /S /Y

::XCOPY .\proto\GameProto.java ..\..\..\..\game\src\main\java\msg\*.* /S /Y

::XCOPY .\proto\HallProto.java ..\..\..\..\hall\src\main\java\msg\*.* /S /Y
pause