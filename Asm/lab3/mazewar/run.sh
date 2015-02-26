#!/bin/bash
JAVA_HOME=/cad2/ece419s/java/jdk1.6.0/

${JAVA_HOME}/bin/java MazewarClient localhost 4444 &
${JAVA_HOME}/bin/java MazewarClient localhost 4444 &
${JAVA_HOME}/bin/java MazewarClient localhost 4444 &
${JAVA_HOME}/bin/java MazewarServer 4444
