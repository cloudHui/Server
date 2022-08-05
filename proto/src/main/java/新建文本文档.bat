
protoc.exe --java_out=.  common.proto


XCOPY .\proto\CommonProto.java ..\..\..\..\..\gate\src\main\java\proto\*.* /S /Y


pause