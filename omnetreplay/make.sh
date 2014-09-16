make clean
rm -r ./lib/rapidjson
unzip -d ./lib ./lib/rapidjson-0.11.zip
opp_msgc .
opp_makemake -f
make
./omnetreplay
