#!/bin/bash
if [[ $# != 1 ]]; then
	echo "Lanciare con: $0 [path di un file]"
	exit 1
fi

java -jar Server.jar &
pidServer=$!
sleep 5
java -jar Client.jar TEST reg a b es log a b searchusr b flist addfriend b msgto b Hola11 flist addfriend b msgto b Hola12 creategrp test joingrp test grplist grplist joingrp test msggrp test testa fileto b $1 msggrp test testa msgto b Hola13 closegrp test msgrp test testa creategrp test &
pid1=$!
java -jar Client.jar TEST reg b b it log b b searchusr a flist addfriend a msgto a Hola21 flist addfriend a msgto a Hola22 creategrp test joingrp test grplist grplist joingrp test msggrp test testb fileto a $1 addfriend c msggrp test testb msgto a Hola23 closegrp test msgrp test testb creategrp test &
pid2=$!
java -jar Client.jar TEST reg c b en log c b flist grplist searchusr a searchusr b addfriend a addfriend b msgto b Hello31 msgto a Hello32 creategrp test2 grplist msggrp test2 testc msggrp test testc addfriend d close &
pid3=$!
java -jar Client.jar TEST reg d b it log d b addfriend c flist grplist joingrp test2 joingrp test msggrp test testd msgto c Ciao41 msgto a Ciao42 fileto c $1 addfriend e close &
pid4=$!
wait $pid1
wait $pid2
wait $pid3
wait $pid4
kill -9 $pidServer