classpath=".\
:./bin/*\
:./lib/*\
:/home/vstone/lib/*\
:/home/vstone/vstonemagic/*\
"

cd ./src

javac -encoding utf-8 -classpath  "$classpath" -d ../bin $1

cd ../bin

