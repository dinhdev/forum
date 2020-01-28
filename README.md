# JForum

# - Basic configuration for integrating between dataexo and sso-auth
This is jforum application.
You have to change some configuration on this. (SystemGlobals.properties)

1. open the configuration file using some of the text editor. (WEB-INF/config/SystemGlobals.properties)
2. you have to change some values

ssoauth.domain = http://localhost:8081/jforum
ssoauth.dataexo = http://localhost:8086
ssoauth.authurl = http://localhost:8087/sso/athenticate
ssoauth.loginurl = http://localhost:8087/jforum/sso/login
ssoauth.logout = http://localhost:8087/sso/logout

  ssoauth.domain is for jforum website url.
  ssoauth.dataexo is for dataexo website url.
  ssoauth.authurl , ssoauth.loginurl , ssoauth.logout are for sso-auth website url. (you have to change only domain address)
  
  
# - How to run the project.

1. download the project to local pc.
2. mvn clean package -DskipTests
3. After this command  , you can see target/jforum-1.0.war file.
4. copy this file to apache-tomcat and start tomcat service.
5. Enter this url on browser: http://localhost:8085/forums/list.page