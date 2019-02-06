# Mem Ory
A Telegram bot for storing and manipulating data. You can find him [here](https://t.me/mem_ory_already_taken_why_bot).

## Guide for ...
* ### Users
    This bot is being developed too quickly to give you any guides right now. The main idea is to implement something like a file system that will take all the advantages of being a Telegram bot: security, uniform GUI on all platforms, caching data for offline usage, data exchange between users... 

* ### Developers
    #### Used technologies
    The bot is written on Java with usage of [TelegramBots](https://github.com/rubenlagus/TelegramBots "Java library to create bots using Telegram Bots API") library of version 4.1 and [Spring Boot](https://spring.io/projects/spring-boot) framework of version 2.0.8.
    
    [PostgreSQL 10.6](https://www.postgresql.org/docs/10/index.html) is used for storing data.
    [Spring Data JPA](http://spring.io/projects/spring-data-jpa) is used for accessing data.

    #### Bot
    For security reasons, the token of this bot is not uploaded. In order to check your code, create your own bot [here](http://t.me/BotFather "BotFather").
    #### Property file
    All the properties are held in Spring's property file ``application.yaml``. This is what it should look like:
    ```
    #this is needed for setting up a connection with database
    #forget about xml configuration
    spring:
      jpa:
        properties:
          hibernate:
            jdbc:
              lob:
                non_contextual_creation: true
    app:
      datasource:
        jdbc-url: jdbc:postgresql://localhost:5432/your_database
        username: your_username
        password: your_password
    
    bot:
      token: 123456789:ExampleOfBotToken
      username: Your bot username
      proxy:
        needed: false
    ```
    If you need proxy, make your bot properties look like these:
    ```
    bot:
      token: 123456789:ExampleOfBotToken
      username: Your bot username
      proxy:
        needed: true
        config:
          connect-timeout: 5000  #the timeout for proxy to respond in milliseconds, default is 5000
          update-delay: 60          #in this example, availableness of the proxies will be updated every 60 seconds, default is 600
        list:
          - 123.45.678.9:1234
          - 98.765.4.321:4321
    ```
    Proxies are checked for availability (this means that they are reachable and you access https://api.telegram.org through them in ``connect-timeout`` milliseconds or faster) automatically every ``update-delay`` seconds. 