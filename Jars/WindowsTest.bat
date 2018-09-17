@echo off
IF "%~1"=="" (
	echo Lanciare con: %~n0 [path di un file]
	exit /B 0
)
START java -jar Server.jar
timeout 5
START /B java -jar Client.jar TEST reg a b es log a b searchusr b flist addfriend b msgto b Hola11 flist addfriend b msgto b Hola12 creategrp test joingrp test grplist grplist joingrp test msggrp test testa fileto b %1 msggrp test testa msgto b Hola13 closegrp test msgrp test testa creategrp test
START /B java -jar Client.jar TEST reg b b it log b b searchusr a flist addfriend a msgto a Hola21 flist addfriend a msgto a Hola22 creategrp test joingrp test grplist grplist joingrp test msggrp test testb fileto a %1 addfriend c msggrp test testb msgto a Hola23 closegrp test msgrp test testb creategrp test
START /B java -jar Client.jar TEST reg c b en log c b flist grplist searchusr a searchusr b addfriend a addfriend b msgto b Hello31 msgto a Hello32 creategrp test2 grplist msggrp test2 testc msggrp test testc addfriend d close
START /B java -jar Client.jar TEST reg d b it log d b addfriend c flist grplist joingrp test2 joingrp test msggrp test testd msgto c Ciao41 msgto a Ciao42 fileto c %1 addfriend e close