#! /bin/bash
while getopts 'abc:h' opt; do
  case "$opt" in
    civ)
      echo "Building Civ AAR"
      ./gradlew clean publish assembleCivDebug
      ;;

    mil)
      echo "Building Mil AAR"
      ./gradlew clean publish assembleMilDebug
      ;;

    ?|h)
      echo "Usage: $(basename $0) [-civ] [-mil]"
      exit 1
      ;;
  esac
done
if [ $# -eq 0 ]; then
    echo "Building Civ AAR"
    ./gradlew clean assembleCivDebug
fi
shift "$(($OPTIND -1))"
cp app/build/outputs/aar/*.aar takml-android-2.2.aar
echo "Done! Built takml-android-2.2.aar"
