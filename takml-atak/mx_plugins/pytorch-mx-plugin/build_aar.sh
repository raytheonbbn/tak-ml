#! /bin/bash
while getopts 'abc:h' opt; do
  case "$opt" in
    civ)
      echo "Building Civ AAR"
      ./gradlew clean assembleCivDebug
      ;;

    mil)
      echo "Building Mil AAR"
      ./gradlew clean assembleMilDebug
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
cp app/build/outputs/aar/*.aar pytorch-lite-1.13.0.aar
echo "Done! Built pytorch-lite-1.13.0.aar"
