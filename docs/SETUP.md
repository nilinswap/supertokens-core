# Setup

1. Install supertokens-root, it installs supertokens-core for you. so clone from [supertokens-root](https://github.
   com/supertokens/supertokens-root)
2. Next follow CONTRIBUTING.md from supertokens-core. it tells how to setup supertokens-root which sets up #!
   supertokens-core inside supertokens-root. then open supertokens-root in intellij
3. java 15 is a must. I set up jdk as 15 in intellij still it did not work. probably because I am using in java1.8 
   as default.(update: yeah, even after making 15 as default, it did not work. I had to manually go to settings for 
   gradle using [this](https://www.jetbrains.com/help/idea/gradle-jvm-selection.html#jvm_settings) )
   so I had to set PATH and JAVA_HOME manually to bring 
   the effect. 
   after which `./startTestEnv --wait` 
   builds successfully. finally ran using java command.
4. CONTRIBUTING.md is complete, no need to look elsewhere.
5. to run it for debugging run it like (using [this](https://www.baeldung.com/java-application-remote-debugging))
```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:6060 -classpath "./core/*:./plugin-interface/*" io.supertokens.Main ./ DEV
```
and go to intellij's Run and select attach to process. 





# note - for just running without the modification use docker-compose. - as [here](https://supertokens.com/blog/connect-supertokens-to-database#running-supertokens-and-postgresql-with-docker-with-docker-compose)

```yml
version: '3'

services:
  db:
    image: 'postgres:latest'
    environment:
      POSTGRES_USER: supertokens_user 
      POSTGRES_PASSWORD: somePassword 
      POSTGRES_DB: supertokens
    ports:
      - 5432:5432
    networks:
      - app_network
    restart: unless-stopped
    healthcheck:
      test: ['CMD', 'pg_isready -U supertokens_user']
      interval: 5s
      timeout: 5s
      retries: 5

  supertokens:
    image: registry.supertokens.io/supertokens/supertokens-postgresql
    depends_on:
      - db
    ports:
      - 3567:3567
    environment:
      POSTGRESQL_CONNECTION_URI: "postgresql://supertokens_user:somePassword@db:5432/supertokens"
      LOG_LEVEL: DEBUG
    networks:
      - app_network
    restart: unless-stopped
    healthcheck:
      test: >
        bash -c 'exec 3<>/dev/tcp/127.0.0.1/3567 && echo -e "GET /hello HTTP/1.1\r\nhost: 127.0.0.1:3567\r\nConnection: close\r\n\r\n" >&3 && cat <&3 | grep "Hello"'
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  app_network:
    driver: bridge
  
```