currdir:=$(shell pwd) 
workdir:=$(shell echo ${currdir}/rpmbuild | tr -d ' ')

all: rpm

rpm:
	mkdir -p ./rpmbuild/{BUILD,RPMS,SOURCES,SPECS,SRPMS}
	echo '%_topdir ${workdir}' > ~/.rpmmacros
	rpmbuild -ba nfn-scala.spec
#	cp rpmbuild/RPMS/

clean:
	rm -rf ./rpmbuild
