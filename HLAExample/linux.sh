#!/bin/bash

USAGE="usage: linux.sh [execute [federate-name]]"

################################
# check command line arguments #
################################
if [ $# = 0 ]
then
	echo $USAGE
	exit;
fi

######################
# test for JAVA_HOME #
######################
JAVA=java
if [ "$JAVA_HOME" = "" ]
then
	echo WARNING Your JAVA_HOME environment variable is not set!
	#exit;
else
        JAVA=$JAVA_HOME/bin/java
fi

#####################
# test for RTI_HOME #
#####################
if [ "$RTI_HOME" = "" ]
then
	cd ..
	RTI_HOME=$PWD
	export RTI_HOME
	cd HLAExample
	echo WARNING Your RTI_HOME environment variable is not set, assuming $RTI_HOME
fi


############################################
### (target) execute #######################
############################################
if [ $1 = "execute" ]
then
	shift;
	java -cp ./dist/HLAExample.jar:$RTI_HOME/lib/portico.jar edu.nps.moves.RprFomFederate $*
	exit;
fi

echo $USAGE