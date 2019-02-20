# Mem Ory
A Telegram bot for storing, structuring and sharing your data. You can find him [here](https://t.me/mem_ory_already_taken_why_bot).
The main idea is to implement a label-based file system that will take all the advantages of being a Telegram bot: security, uniform GUI on all platforms, caching data for offline usage, data exchange between users...<br /> **"Label-based"** means that you search for your file using labels you assigned to it, just like tags. We all are used to the so-called **hierarchical file system** - that is when you put files into folders, then you put that folders into another folders and so on. Hierarchical file system is somewhat opposite to this one.<br />What is the purpose for this kind of file system? Imagine the following situation: you have found files of four types on the Internet: videos for studying, books for studying, videos for fun and books for fun. Now you want to save them, but you also want to store them in thematical folders. How would you call that folders? There are two obvious options:<br/>

| Option 1 | Option 2 |
| ------------- | ------------- |
| Books<ul><li>Fun</li><li>Study</li></ul>Video<ul><li>Fun</li><li>Study</li></ul>| Fun<ul><li>Books</li><li>Video</li></ul>Study<ul><li>Books</li><li>Video</li></ul>|

The problem here is that these two options are equally good. The ambiguity of choosing one of them leads to difficulties while trying to find that files. It is pretty simple in this example, but it becomes more difficult with more directories.<br />The other possible choice is to have all four directories top-level and store all fun content in Fun directory, all videos in Video, etc. This is more like a **label-based** system, because you are now able to find a fun book in both Books and Fun directories, which are independent. Just like tags.<br /> However, that way leads to storing multiple copies of each file, but there is usually a limit of space that prevents you from doing so. **Usually, but not today.** 

## Guide for ...
* ### Users
    [This step-by-step guide](https://github.com/borisPristupa/Mem-Ory/wiki/Step-by-step-guide) with screenshots will take you through the power of this bot.

* ### My university's practice lead
    Let's be honest: this is a wonderful project. In case you have got any commentaries, contact me [here](https://github.com/borisPristupa/Mem-Ory#contact-me)

* ### Developers
    #### Architecture
    The main concept of this app is based on Scenarios - classes, extending [``BotScenario``](https://github.com/borisPristupa/Mem-Ory/blob/master/src/main/java/com/boris/study/memory/logic/sructure/BotScenario.java) abstract class. Each such class contains a method ``process``, receiving ``Update`` and a ``Map<String, String>``, performing some business logic and returning ``Boolean`` - an answer to the question "Did this scenario finish?". <br/>You might be interested why do we need returning ``Boolean`` to determine whether the scenario has finished or not, why don't we just assume it has finished after ``process`` method returned? That is due to the design of the TelegramBots library: each bot has just one method that receives updates asynchronously. But what if you need to have a conversation with your user whithin a single scenario? It was decided to do in this way: when scenario's code finishes, but it still has some logic to finish, it returns false, which indicates that the one who started this scenario should pass the information about new updates right to this scenario untill it returns true.
    #### Used technologies
    The bot is written on Java with usage of [TelegramBots](https://github.com/rubenlagus/TelegramBots "Java library to create bots using Telegram Bots API") library of version 4.1 and [Spring Boot](https://spring.io/projects/spring-boot) framework of version 2.0.8.
    
    [PostgreSQL 10.6](https://www.postgresql.org/docs/10/index.html) is used for storing data.<br />[Spring Data JPA](http://spring.io/projects/spring-data-jpa) is used for accessing data.

    #### Bot
    For security reasons, some properties (i.e. token) of this bot are not uploaded. In order to check your code, create your own bot [here](http://t.me/BotFather "BotFather"). You will also need to add your bot to a conversation of two or more people. The bot will send its data there. The id of that chat should be placed into the property file as ``app.magic-chat``.
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
      magic-chat: -123456732
    
    bot:
      token: 123456789:ExampleOfBotToken
      username: Your bot username
      proxy:
        needed: false
    ```
    Proxies now don't work for an unknown reason. **Use VPN**
    
## Contact me
Gmail: boris.pristupa@gmail.com<br/>Telegram: [@boris_pristupa](http://telegram.me/boris_pristupa)<br/>VK: [Борис Приступа](http://vk.com/boris_pristupa)
