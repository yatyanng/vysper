# This is a clone of apache vysper 7.0 - open source xmpp server in java based on apache mina.

https://mina.apache.org/vysper-project/index.html

The source is modified such that jitsi components (jvb and jicofo) can communicate through it.

For jitsi, see https://jitsi.org/jitsi-meet/

# Important: The certificate file specified in spring-config.xml should match the trust store specified in jicofo.sh.

# Example

##spring-config.xml
```
  <property name="certificateFile" value="file:///opt/vysper-0.7/config/vysper.jks" />
  <property name="certificatePassword" value="vysper" />
```

##jicofo.sh
```
 -Djavax.net.ssl.trustStore=/opt/vysper-0.7/config/vysper.jks -Djavax.net.ssl.trustStorePassword=vysper
```