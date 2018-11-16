dialog-share-bot
=================

## Compilation

In order to run this bot you should get [java-bot-sdk](https://github.com/nabetse00/java-bot-sdk/) from my repo until
changes are merge to master.

To keep it simple:
1. make a directory `mkdir JAVA_SHARE_BOT_DIR`
2. Clone my sdk repo `git clone https://github.com/nabetse00/java-bot-sdk.git`
3. Cd into `java-bot-sdk` directory
4. Inside `java-bot-sdk` clone this git repo: `git clone https://github.com/nabetse00/dialog-share-bot.git`
5. Then run: `gradlew build` on linux or `gradlew.bat build` on windows.


## Launch

To run the bot: `gradlew run` on linux or `gradlew.bat run` on windows.
Don't forget to fill your bot token on Main.java and check endpoint configuration on [dialog.conf](dialog-share-bot/src/main/resources/dialog.conf):
This will run Main class main method.

Don't forget to update required values and/or modify this file to your needs.

```java
    public static void main(String[] args) {

        try {
            String shareBotName = "Botname";
            String myToken = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            String shareBotTwitterAccount = "twitterAccount"; // just account name no @
            new ShareBot(myToken, shareBotName, shareBotTwitterAccount);
            // if no twitter account just:
            // new ShareBot(myToken, shareBotName);
            // if no name to share bot:
            // new ShareBot(myToken);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
```

## Deployment

[dialog.conf](dialog-share-bot/src/main/resources/dialog.conf) contains endpoint and host info:

```json
dialog.botsdk {
  host = grpc-test.transmit.im
  port = 8080
  fork-join-pool.parallelism = 16
}
```



## Built With

* [Visual Studio Code](https://code.visualstudio.com/) - IDE
* [Gradle](https://gradle.org/) - Dependency Management
* [slf4j](https://www.slf4j.org/) - Simple Logging Facade for Java

## Authors

* [**Nabetse**](https://iteasys.com) 

See also the list of [contributors](https://github.com/nabetse00/java-bot-sdk/graphs/contributors) who participated in this project.

## License

This project is licensed under Apache V2 - see the [LICENSE.md](dialog-share-bot/LICENSE) file for details.