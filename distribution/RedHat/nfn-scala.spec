Name:		NFN-Scala
Version:	0.2.0
Release:	1%{?dist}
Summary:	NFN-Scala

Group:		Application/Internet
License:	ISC
URL:		ccn-lite.net

#BuildRequires:	
#Requires:	

%description
NFN-scala is a Service layer implementation for CCN-lite 

#%prep
#%setup -q
#copy ccn-lite src into dir, or check it out?

#%build
#%configure
#make %{?_smp_mflags}
#make the bin files, 

%install
#%make_install
#use a make install script, which matches the requirements (install from bin...)
mkdir ../BUILDROOT/%name-%version-%release.%_arch/usr
mkdir ../BUILDROOT/%name-%version-%release.%_arch/usr/local
mkdir ../BUILDROOT/%name-%version-%release.%_arch/usr/local/bin
cp  ../../../../target/scala-2.10/nfn.* ../BUILDROOT/%name-%version-%release.%_arch/usr/local/bin/nfn.jar

cp  ../../../script/nfn ../BUILDROOT/%name-%version-%release.%_arch/usr/local/bin/nfn

%files
%doc
/usr/local/bin/nfn*


%changelog

