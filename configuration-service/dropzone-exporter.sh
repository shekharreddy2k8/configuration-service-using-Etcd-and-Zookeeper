#!/bin/bash -x

if [ $# -lt 3 ]; then
  echo "Usage: `basename $0` <version string> <dropzone dir> <timestamp in ISO8601 format>"
  exit 1
fi

VERSION=$1

DROPZONE_DIR="$2"
theOS=`uname -s | cut -d- -f1`
if [ "$theOS" = "CYGWIN_NT" ]; then
      # convert Windows platform filename format to one Cygwin understands
      DROPZONE_ROOT=`cygpath -u "$DROPZONE_DIR"`
      echo "Adjusted DROPZONE_ROOT is $DROPZONE_ROOT"
else
      DROPZONE_ROOT="$DROPZONE_DIR"
      echo "Keeping DROPZONE_ROOT as $DROPZONE_ROOT"
fi

DROPZONE_DCM="$DROPZONE_ROOT"/DCM/UnofficialBuilds/MAS-${VERSION}

TIMESTAMP=`echo $3 | cut -c -19 | sed -e 's,:,,g' -e 's,-,,g' -e 's,T,,g'`
DROPZONE_NOW=${DROPZONE_DCM}/${TIMESTAMP}



#
# copy installer bits
#
mkdir -p ${DROPZONE_NOW}
cp ./target/rpm/ConfigurationService/RPMS/noarch/* ${DROPZONE_NOW}/
echo "" >> ${DROPZONE_NOW}/VERSION
echo -e "||filename\t||md5\t||checksum\t||ls -lrt (size)\t||" > header.out
for i in `find ${DROPZONE_NOW} -name "*.bin"  -o -name "*.zip" -o -name "*.rpm"`; do
    echo `md5sum $i | sed "s%${DROPZONE_NOW}/%%"` `cksum $i` | awk '{ print "|"$2"\t|"$1"\t|"$3"\t|"$4"\t|" }' >> tmp.out
done
sort tmp.out >> header.out
column -s $'\t' -t header.out >> ${DROPZONE_NOW}/VERSION
rm tmp.out
rm header.out
echo >> ${DROPZONE_NOW}/VERSION

exit 0
