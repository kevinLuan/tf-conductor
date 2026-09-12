cd /Users/kevin/java_home/test/tf-conductor

./gradlew clean build -x test publishToMavenLocal
scp ./server/build/libs/conductor-server-3.30.2-SNAPSHOT-boot.jar root@8.147.111.156:/root/java_apps/conductor/
