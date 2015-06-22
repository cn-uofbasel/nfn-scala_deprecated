section:=base
arch:=amd64
version:=0.2.0
installedSize:=$(shell du -ks nfn-scala/usr|cut -f 1)
year:=$(shell date +"%Y")

all: deb

deb:
	mkdir nfn-scala
	mkdir nfn-scala/DEBIAN
	mkdir nfn-scala/usr
	mkdir nfn-scala/usr/local
	mkdir nfn-scala/usr/local/bin
	cp -L ../../target/scala-2.10/nfn* nfn-scala/usr/local/bin/nfn.jar
	cp ../script/nfn nfn-scala/usr/local/bin/nfn
	cp Metadata nfn-scala/DEBIAN/control
	cp License nfn-scala/DEBIAN/copyright
	sed -i 's/<%= version %>/${version}/g' nfn-scala/DEBIAN/control
	sed -i 's/<%= section %>/${section}/g' nfn-scala/DEBIAN/control
	sed -i 's/<%= arch %>/${arch}/g' nfn-scala/DEBIAN/control
	sed -i 's/<%= installedSize %>/${installedSize}/g' nfn-scala/DEBIAN/control
	sed -i 's/<%= year %>/${year}/g' nfn-scala/DEBIAN/control
	sed -i 's/<%= year %>/${year}/g' nfn-scala/DEBIAN/copyright
	dpkg --build nfn-scala
	rm -r ./nfn-scala	
	
clean:
	rm -rf ./nfn-scala
	rm -f ./nfn-scala.deb
