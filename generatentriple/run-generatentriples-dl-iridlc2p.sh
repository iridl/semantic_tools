#!/bin/csh

#setenv JAVA_HOME /local05/opt/jdk1.6.0_21_64
setenv JAVA_HOME /software/rhel5/x86_64/jdk1.6.0_13
date --iso-8601=minutes
echo Start generatentriples.

/software/rhel5/x86_64/jdk1.6.0_13/bin/java -Xmx4096m -jar /home/haibo/wmsjar/generatentriples.jar -cache=/home/haibo/dlmetadatatest -notimport=/home/haibo/urltoskip-dl.txt -dropChoice=dropWithMemoryStoreDeleteRepository http://iridl.ldeo.columbia.edu/ontologies/iridlmetadata.owl >dl.log

#/local05/opt/jdk1.6.0_21/bin/java -Xmx3072m -jar /home/haibo/wmsjar/generatentriples.jar -cache=/home/haibo/dlmetadatatest -notimport=/home/haibo/urltoskip-dl.txt -dropChoice=dropWithMemoryStoreDeleteRepository http://iridl.ldeo.columbia.edu/ontologies/iridlmetadata.owl >dl.log

#/local05/opt/jdk1.6.0_21/bin/java -Xmx3072m -jar /data/haibo/wmsjar/generatentriples.jar -sesame=http://iridlc6.ldeo.columbia.edu:8480/openrdf-sesame  -repository=dl-owl -notimport=/home/haibo/urltoskip-dl.txt http://iridl.ldeo.columbia.edu/ontologies/iridlmetadata.owl >dl.log

date --iso-8601=minutes
echo Finish generatentriples.
sleep 60  #delay 1 minute

set finishTime=`date +%Y%m%d%H%M`
echo Finish time $finishTime

echo Start clearing repository... 
/data/jdcorral/Sesame2/bin/sesame2_console.sh <<EOT
connect http://iridlc6.ldeo.columbia.edu:8480/openrdf-sesame.
open dl-owl .
clear .
exit.
EOT
date --iso-8601=minutes

echo Start loading repository... 
cd /data/jdcorral/Sesame2/bin/
/data/jdcorral/Sesame2/bin/sesame2_console.sh <<EOT
connect http://iridlc6.ldeo.columbia.edu:8480/openrdf-sesame.
open dl-owl .
clear .
load /home/haibo/dlmetadatatest/owlimMaxRepository.nt.
EOT

echo loading /home/haibo/dlmetadatatest/owlimMaxRepository.nt.
date --iso-8601=minutes
echo Finish loading repository.
cd /home/haibo

echo archiving logs...
mv /home/haibo/dl.log               /home/haibo/dl.log$finishTime
mv /home/haibo/irisemantics.log     /home/haibo/irisemantics.log$finishTime
mv /home/haibo/opendapsemantics.log /home/haibo/opendapsemantics.log$finishTime
#mv /home/haibo/dlmetadatatest       /home/haibo/dlmetadatatest$finishTime

#scp /home/haibo/*.log$finishTime haibo@bubba:/local/scr/
#scp /home/haibo/dlmetadatatest$finishTime haibo@bubba:/local/scr/
#mv /home/haibo/dl.log$finishTime               /home/haibo/dlmetadatatest$finishTime
#mv /home/haibo/irisemantics.log$finishTime     /home/haibo/dlmetadatatest$finishTime/
#mv /home/haibo/opendapsemantics.log$finishTime /home/haibo/dlmetadatatest$finishTime/
