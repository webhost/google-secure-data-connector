Summary: Google Secure Data Connector Agent
Name: google-secure-data-connector
Version: __VERSION__
Release: 2
Source0: %{name}-%{version}-%{release}-bin.tar.gz 
License: GPL
Group: Administrative/System
Requires: 
BuildArch: noarch
BuildRoot: %_topdir/BUILD/%{name}-root

%description
Google Secure Data Connector Agent



%preun

%pre 

%prep 
#%setup -c -n %{name}-%{version} 
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/source
tar -xzf $RPM_SOURCE_DIR/%{name}-%{version}-%{release}-src.tar.gz -C $RPM_BUILD_ROOT/source

%build
cd $RPM_BUILD_ROOT/source/data-connector-agent

./configure.sh \
  --lsb \
  --noverify \
  --user=securedataconnector \
  --group=securedataconnector \
  --javahome=/usr

ant 

cd distribute
ant binary-distro
cp dist/%{name}-%{version}-%{release}-bin.tar.gz $RPM_SOURCE_DIR/%{name}-%{version}-%{release}-bin.tar.gz

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/opt/%{name}
mkdir -p $RPM_BUILD_ROOT/opt/%{name}/lib
mkdir -p $RPM_BUILD_ROOT/opt/%{name}/bin
mkdir -p $RPM_BUILD_ROOT/etc/opt/%{name}
# mkdir -p $RPM_BUILD_ROOT/etc/opt/%{name}/jsw
tar -xzf $RPM_SOURCE_DIR/%{name}-%{version}-%{release}-bin.tar.gz -C $RPM_BUILD_ROOT/opt/%{name}

# Cleanup files not needed for RPM
if [ -e $RPM_BUILD_ROOT/opt/%{name}/configure.sh ]; then
  rm -f $RPM_BUILD_ROOT/opt/%{name}/configure.sh
fi


# Moving config files for /etc/opt/${name}
mv $RPM_BUILD_ROOT/opt/%{name}/config/localConfig.xml $RPM_BUILD_ROOT/etc/opt/%{name}/localConfig.xml
mv $RPM_BUILD_ROOT/opt/%{name}/config/resourceRules.xml $RPM_BUILD_ROOT/etc/opt/%{name}/resourceRules.xml
mv $RPM_BUILD_ROOT/opt/%{name}/config/log4j.properties $RPM_BUILD_ROOT/etc/opt/%{name}/log4j.properties

# Relocate runclient.sh, start.sh, stop.sh
mv $RPM_BUILD_ROOT/opt/%{name}/runclient.sh $RPM_BUILD_ROOT/opt/%{name}/bin
mv $RPM_BUILD_ROOT/opt/%{name}/start.sh $RPM_BUILD_ROOT/opt/%{name}/bin
mv $RPM_BUILD_ROOT/opt/%{name}/stop.sh $RPM_BUILD_ROOT/opt/%{name}/bin

# Relocate core jars
mv $RPM_BUILD_ROOT/opt/%{name}/build/agent.jar $RPM_BUILD_ROOT/opt/%{name}/lib
mv $RPM_BUILD_ROOT/opt/%{name}/build/protocol.jar $RPM_BUILD_ROOT/opt/%{name}/lib
rm -rf $RPM_BUILD_ROOT/opt/%{name}/build

# Relocate third-party path
mv $RPM_BUILD_ROOT/opt/%{name}/third-party $RPM_BUILD_ROOT/opt/%{name}/lib


# Removing source config directory
rm -rf $RPM_BUILD_ROOT/opt/%{name}/config

%post 
set -e
CLIENT_USER=securedataconnector
CLIENT_GROUP=securedataconnector
CONF_DIR="/etc/opt/%{name}"
CLIENT_HOME="/opt/%{name}"
LOGDIR="/var/opt/%{name}/log"

# Set up the user to access the client
adduser -s /bin/bash  -M $CLIENT_USER

dd if=/dev/urandom bs=1024 count=1 |passwd --stdin $CLIENT_USER

  # Set up the logfile for %{name}
  if [ ! -d $LOGDIR ]; then
    mkdir -p $LOGDIR
  fi
  chown root:$CLIENT_GROUP $LOGDIR
  chmod 770 $LOGDIR
  for filename in "agent"; do
    logfile="$LOGDIR/$filename"
    if [ ! -f $logfile ]; then
      touch $logfile
    fi
    chown root:$CLIENT_USER $logfile
    chmod 660 $logfile
  done

  chmod 750 /etc/opt/%{name}
  chown -R root:$CLIENT_USER /etc/opt/%{name}
  chown -R root:$CLIENT_USER /opt/%{name}

  chown -R root:$CLIENT_GROUP /etc/opt/%{name}
  chown -R root:$CLIENT_GROUP /etc/opt/%{name}/localConfig.xml
  chown -R root:$CLIENT_GROUP /etc/opt/%{name}/resourceRules.xml

  chmod 640 /etc/opt/%{name}/localConfig.xml
  chmod 640 /etc/opt/%{name}/resourceRules.xml

  chown -R root:$CLIENT_GROUP /etc/opt/%{name}/resourceRules.xml

  # Check for java
  echo "Checking for Java..."
  if [ ${JAVA_HOME} ]; then
    JAVABIN=${JAVA_HOME}/bin/java
  elif [ ${JAVAHOME} ]; then
    JAVABIN=${JAVAHOME}/bin/java
  else # Try to figure it out.
    JAVABIN=$(which java) >/dev/null 2>&1 || true
  fi

  if [ -x "${JAVABIN}" ]; then
    ${JAVABIN} -version 2>&1 | grep 'version' |grep -q '1.6'
    if [ $? != 0 ]; then
      echo "Java found at ${JAVABIN} might not suitable."
      echo "Secure Data Connector requires JRE 1.6 or higher"
      exit 0
    else
      echo "Found java at ${JAVABIN}"
    fi
  else
    echo "Java could not be found."
    echo "Please install Java JRE 1.6 or higher before using the Secure Data Connector."
    exit 0
  fi



%postun
echo "Removing User Accounts.."

userdel -r woodstock || true
userdel -r securedataconnector || true

%clean                               
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root) 
%config /etc/opt/%{name}/resourceRules.xml
%config /etc/opt/%{name}/localConfig.xml
/etc/opt/%{name}/*
/opt/%{name}/*  



