#!/bin/bash -ex

### This script creates a Domibus image based on Ubuntu 18.04 ###

export DEBIAN_FRONTEND=noninteractive
sudo sed -i "s/127.0.0.1 localhost/127.0.0.1 localhost $(hostname)/g" /etc/hosts
sudo apt-get update -q
sudo apt-get install dialog

######### Install & configure JRE ########
sudo add-apt-repository ppa:webupd8team/java --yes
sudo apt-get update
echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections
DEBIAN_FRONTEND=noninteractive sudo apt install openjdk-8-jre-headless --yes --force-yes --assume-yes

JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre

sudo echo >> /home/ubuntu/.bashrc
sudo echo "JAVA_HOME=$JAVA_HOME" >> /home/ubuntu/.bashrc

source /home/ubuntu/.bashrc

######### Install MySql ##########

sudo debconf-set-selections <<< 'mysql-server-5.7 mysql-server/root_password password root'
sudo debconf-set-selections <<< 'mysql-server-5.7 mysql-server/root_password_again password root'
sudo apt-get -y install mysql-server-5.7

########## Create domibus db and user ########
mysql -h localhost -u root --password=root -e "drop schema if exists domibus; create schema domibus; alter database domibus charset=utf8; create user edelivery identified by 'edelivery'; grant all on domibus.* to edelivery;"

########## Install Domibus ########
export DOMIBUS_VERSION="4.0"

export TOMCAT_MAJOR="8"
export TOMCAT_VERSION="8.0.24"

export MYSQL_CONNECTOR="mysql-connector-java-5.1.40"
export DOMIBUS_DIST="/usr/local/domibusDist"
export TOMCAT_FULL_DISTRIBUTION="/usr/local/tomcat"
export CATALINA_HOME="$TOMCAT_FULL_DISTRIBUTION/domibus"

export ADMIN_USER="admin"
export ADMIN_PASSW="123456"

sudo apt-get install -y wget
sudo apt-get install -y unzip

sudo wget https://ec.europa.eu/cefdigital/artifact/content/repositories/public/eu/domibus/domibus-distribution/$DOMIBUS_VERSION/domibus-distribution-$DOMIBUS_VERSION-tomcat-full.zip \
    && sudo unzip -o -d $TOMCAT_FULL_DISTRIBUTION domibus-distribution-$DOMIBUS_VERSION-tomcat-full.zip

sudo wget https://ec.europa.eu/cefdigital/artifact/content/repositories/public/eu/domibus/domibus-distribution/$DOMIBUS_VERSION/domibus-distribution-$DOMIBUS_VERSION-sample-configuration-and-testing.zip \
    && sudo unzip -o -d $DOMIBUS_DIST domibus-distribution-$DOMIBUS_VERSION-sample-configuration-and-testing.zip

sudo wget https://dev.mysql.com/get/Downloads/Connector-J/$MYSQL_CONNECTOR.zip \
    && sudo unzip -o -d $DOMIBUS_DIST $MYSQL_CONNECTOR.zip

sudo cp $DOMIBUS_DIST/$MYSQL_CONNECTOR/$MYSQL_CONNECTOR-bin.jar $CATALINA_HOME/lib
sudo cp -R $DOMIBUS_DIST/conf/domibus/keystores $CATALINA_HOME/conf/domibus/

sudo chmod 777 $CATALINA_HOME/bin/*.sh
sudo sed -i 's/\r$//' $CATALINA_HOME/bin/setenv.sh
sudo sed -i 's/#JAVA_OPTS/JAVA_OPTS/g' $CATALINA_HOME/bin/setenv.sh

sudo tar xzf data.tgz

########## Create domibus tables ########
mysql -h localhost -u root --password=root domibus < $TOMCAT_FULL_DISTRIBUTION/sql-scripts/mysql5innoDb-4.0.ddl
mysql -h localhost -u root --password=root domibus < $TOMCAT_FULL_DISTRIBUTION/sql-scripts/mysql5innoDb-4.0-data.ddl

sudo sed -i 's/gateway_truststore.jks/ceftestparty9gwtruststore.jks/g' $CATALINA_HOME/conf/domibus/domibus.properties
sudo sed -i 's/gateway_keystore.jks/ceftestparty9gwkeystore.jks/g' $CATALINA_HOME/conf/domibus/domibus.properties
sudo sed -i 's/blue_gw/ceftestparty9gw/g' $CATALINA_HOME/conf/domibus/domibus.properties

sudo mv data/domibus-ceftestparty9gw-pmode.xml $DOMIBUS_DIST/conf/pmodes/
sudo mv data/* $CATALINA_HOME/conf/domibus/keystores/

sudo chown -R ubuntu:ubuntu $CATALINA_HOME

########## Configure Tomcat as a service ###########
sudo bash -c 'cat << EOF > /etc/init.d/domibus
#!/bin/bash
#
### BEGIN INIT INFO
# Provides:          domibus
# Required-Start:
# Required-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:
# Short-Description: Domibus ...
### END INIT INFO

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre

case \$1 in
        start)
		echo "start .... "
            sh /usr/local/tomcat/domibus/bin/startup.sh
                echo "after start ... "
	    ;;
        stop)
            sh /usr/local/tomcat/domibus/bin/shutdown.sh
            ;;
        restart)
            sh /usr/local/tomcat/domibus/bin/shutdown.sh
            sh /usr/local/tomcat/domibus/bin/startup.sh
            ;;
        esac
            #exit 0
EOF'

sudo chmod 755 /etc/init.d/domibus

sudo ln -s /etc/init.d/domibus /etc/rc1.d/K99domibus
sudo ln -s /etc/init.d/domibus /etc/rc2.d/S99domibus

sudo bash -c 'cat << EOF > /lib/systemd/system/domibus.service
[Unit]
Description = Domibus daemon
After=mysql.service

[Service]
Type=forking
ExecStart=/etc/init.d/domibus start

[Install]
WantedBy=multi-user.target
Alias=domibus.service

EOF'

sudo systemctl enable domibus

######### Start the service #########
cd $CATALINA_HOME
sudo service domibus start

while ! curl --output /dev/null --silent --head --fail http://localhost:8080/domibus; do sleep 1 && echo -n .; done;

echo "   Loging to Domibus to obtain cookies"
curl http://localhost:8080/domibus/rest/security/authentication \
-i \
-H "Content-Type: application/json" \
-X POST -d '{"username":"admin","password":"123456"}' \
-c cookie.txt

JSESSIONID=`grep JSESSIONID cookie.txt |  cut -d$'\t' -f 7`
XSRFTOKEN=`grep XSRF-TOKEN cookie.txt |  cut -d$'\t' -f 7`

echo ; echo
echo "   JSESSIONID=${JSESSIONID}"
echo "   XSRFTOKEN=${XSRFTOKEN}"
echo  "  X-XSRF-TOKEN: ${XSRFTOKEN}"

echo ; echo "   Uploading Pmode"

curl http://localhost:8080/domibus/rest/pmode \
-b cookie.txt \
-v \
-H "X-XSRF-TOKEN: ${XSRFTOKEN}" \
-F  file=@"$DOMIBUS_DIST/conf/pmodes/domibus-ceftestparty9gw-pmode.xml" \
-F  description="Connectivity Test Platform"


